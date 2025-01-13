package org.entcore.conversation.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.*;

import java.util.Optional;
import java.util.function.Function;

import static io.vertx.pgclient.PgConnectOptions.DEFAULT_SSLMODE;

public class ReactivePGClient {

  private final Logger logger = LoggerFactory.getLogger(ReactivePGClient.class);
  private Pool primaryPool;
  private Pool secondaryPool;

  public ReactivePGClient(final Vertx vertx, final JsonObject configuration) {

    JsonObject pgConfig = configuration.getJsonObject("postgresConfig");
    if(pgConfig == null) {
      throw new IllegalArgumentException("'postgresConfig' is missing in 'conversation' module");
    }
    primaryPool = createPool(vertx, pgConfig, "primary").orElseThrow(() -> new IllegalArgumentException("Missing mandatory configuration postgresConfig."));
    secondaryPool = createPool(vertx, pgConfig, "secondary").orElse(primaryPool);
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
      .setSslMode(SslMode.valueOf(pgConfig.getString("ssl-mode", DEFAULT_SSLMODE.name())));

    final PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);
    if(pgConfig.getBoolean("shared", false)) {
      poolOptions.setShared(true)
        .setName(pgConfig.getString("pool-name", "general-" +name + "-pg-pool"));
    }

    return Optional.ofNullable(Pool.pool(vertx, connectOptions, poolOptions));
  }

  public Future<RowSet<Row>> prepared(final String query, final Tuple values, SqlConnection connection) {
    final Promise<RowSet<Row>> promise = Promise.promise();
    connection.preparedQuery(query).execute(values).onComplete(r -> {
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
    return secondaryPool.withConnection(function);
  }
  public <T> Future<T> withReadWriteConnection(Function<SqlConnection,Future<T>> function) {
    return primaryPool.withConnection(function);
  }

  public <T> Future<T> withReadWriteTransaction(final Function<SqlConnection, Future<T>> function) {
    return primaryPool.withTransaction(function);
  }

  public <T> Future<T> withReadOnlyTransaction(final Function<SqlConnection, Future<T>> function) {
    return secondaryPool.withTransaction(function);
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
    return connection.preparedQuery(query.toString()).execute(values);
  }
}
