package org.entcore.conversation.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.vertx.pgclient.PgConnectOptions.DEFAULT_SSLMODE;
import static io.vertx.sqlclient.SqlConnectOptions.DEFAULT_PREPARED_STATEMENT_CACHE_MAX_SIZE;
import static java.lang.System.currentTimeMillis;

public class ReactivePGClient {

  private final Logger logger = LoggerFactory.getLogger(ReactivePGClient.class);
  private Pool primaryPool;
  private Pool secondaryPool;
  private static Timer executionTimer = null;
  private static Timer acquireConnection = null;
  private static Timer acquireTransaction = null;
  private static Counter requestCounter = null;
  /** The probability of choosing the primary source to perform read-only operations (use to improve load balance).*/
  private float primaryRatio;

  public ReactivePGClient(final Vertx vertx, final JsonObject configuration) {
    initMetrics();
    JsonObject pgConfig = configuration.getJsonObject("postgresConfig");
    if(pgConfig == null) {
      throw new IllegalArgumentException("'postgresConfig' is missing in 'conversation' module");
    }
    primaryRatio = pgConfig.getFloat("primary-ratio", 0F);
    primaryPool = createPool(vertx, pgConfig, "primary").orElseThrow(() -> new IllegalArgumentException("Missing mandatory configuration postgresConfig."));
    secondaryPool = createPool(vertx, pgConfig, "secondary").orElse(primaryPool);
  }

  private void initMetrics() {
    if(executionTimer == null) {
      final MeterRegistry registry = BackendRegistries.getDefaultNow();
      if (registry == null) {
        throw new IllegalStateException("micrometer.registries.empty");
      }
      executionTimer = Timer.builder("pg.execution")
        .publishPercentileHistogram(true)
        .maximumExpectedValue(Duration.ofSeconds(1L))
        .register(registry);
      acquireConnection = Timer.builder("pg.acquire.connection")
        .publishPercentileHistogram(true)
        .maximumExpectedValue(Duration.ofMillis(20L))
        .register(registry);
      acquireTransaction = Timer.builder("pg.acquire.transaction")
        .publishPercentileHistogram(true)
        .maximumExpectedValue(Duration.ofMillis(200L))
        .register(registry);
      requestCounter = Counter.builder("pg.requests").register(registry);
    }
  }

  private Optional<Pool> createPool(final Vertx vertx, final JsonObject config,
                                   final String name) {
    JsonObject pgConfig = config.getJsonObject(name);
    if(pgConfig == null) {
        return Optional.empty();
    }
    String url = pgConfig.getString("url", "postgresql://localhost:5432/test").replaceFirst("^jdbc:", "");
    String username = pgConfig.getString("user", "postgres");
    String password = pgConfig.getString("password", "");
    int maxPoolSize = pgConfig.getInteger("pool-size", 10);

    PgConnectOptions connectOptions = PgConnectOptions.fromUri(url)
      .setUser(username)
      .setPassword(password)
      .setSslMode(SslMode.valueOf(pgConfig.getString("ssl-mode", DEFAULT_SSLMODE.name())))
      .setCachePreparedStatements(true)
      .setPreparedStatementCacheMaxSize(pgConfig.getInteger("prepared-statement-cache-max-size", DEFAULT_PREPARED_STATEMENT_CACHE_MAX_SIZE));

    final PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);
    if(pgConfig.containsKey("event-loop-size")) {
      poolOptions.setEventLoopSize(pgConfig.getInteger("event-loop-size"));
    }
    if(pgConfig.getBoolean("shared", false)) {
      poolOptions.setShared(true)
        .setName(pgConfig.getString("pool-name", "general-" +name + "-pg-pool"));
    }

    return Optional.ofNullable(Pool.pool(vertx, connectOptions, poolOptions));
  }

  public Future<RowSet<Row>> prepared(final String query, final Tuple values, SqlConnection connection) {
    final Promise<RowSet<Row>> promise = Promise.promise();
    final long start = currentTimeMillis();
    requestCounter.increment();
    connection.preparedQuery(query).execute(values).onComplete(r -> {
      executionTimer.record(currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      if(r.failed()) {
        logger.error("An error occurred while executing query\nquery:" + query + "\nvalues : " + values.deepToString());
        promise.fail(r.cause());
      } else {
        promise.complete(r.result());
      }
    });
    return promise.future();
  }

  public <T> Future<T> withReadOnlyConnection(Function<SqlConnection,Future<T>> function) {
    final long start = currentTimeMillis();
    Pool pool = (Math.random() < primaryRatio) ? primaryPool : secondaryPool;
    return pool.withConnection(connection -> {
      acquireConnection.record(currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return function.apply(connection);
    });
  }
  public <T> Future<T> withReadWriteConnection(Function<SqlConnection,Future<T>> function) {
    final long start = currentTimeMillis();
    return primaryPool.withConnection(connection -> {
      acquireConnection.record(currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return function.apply(connection);
    });
  }

  public <T> Future<T> withReadWriteTransaction(final Function<SqlConnection, Future<T>> function) {
    final long start = currentTimeMillis();
    return primaryPool.withTransaction(connection -> {
      acquireTransaction.record(currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return function.apply(connection);
    });
  }

  public <T> Future<T> withReadOnlyTransaction(final Function<SqlConnection, Future<T>> function) {
    final long start = currentTimeMillis();
    Pool pool = (Math.random() < primaryRatio) ? primaryPool : secondaryPool;
    return pool.withTransaction(connection -> {
      acquireTransaction.record(currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return function.apply(connection);
    });
  }

  public Future<RowSet<Row>> insert(String tableName, JsonObject data, SqlConnection connection) {
    return insert(tableName, data, null, connection);
  }

  public Future<RowSet<Row>> insert(String tableName, JsonObject data, final String returning, SqlConnection connection) {
    final StringBuilder query = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
    final Tuple values = Tuple.tuple();
    data.stream().forEach(entry -> {
      query.append('"').append(entry.getKey()).append("\",");
      values.addValue(entry.getValue());
    });
    query.setLength(query.length() - 1);
    query.append(") VALUES (");
    for(int i = 1; i <= data.size(); i++) {
      query.append('$').append(i).append(',');
    }
    query.setLength(query.length() - 1);
    query.append(")");
    if(returning != null) {
      query.append(" RETURNING ").append(returning);
    }
    return this.prepared(query.toString(), values, connection);
  }
}
