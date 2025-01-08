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
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Function;

import static io.vertx.pgclient.PgConnectOptions.DEFAULT_SSLMODE;

public class ReactivePGClient {

  private final Logger logger = LoggerFactory.getLogger(ReactivePGClient.class);
  private Pool primaryPool;
  private Pool secondaryPool;

  public ReactivePGClient(final Vertx vertx, final JsonObject configuration) {

    JsonObject pgConfig = configuration.getJsonObject("postgresConfig");
    if(pgConfig == null) {
      final String rawConf = (String) vertx.sharedData().getLocalMap("server").get("postgresConfig");
      if (rawConf != null) {
        pgConfig = new JsonObject(rawConf);
      }
    }
    String primaryUrl = pgConfig.getString("url", "postgresql://localhost:5432/test").replaceFirst("^jdbc:", "");
    String secondaryUrl = pgConfig.getString("url-slave", "").replaceFirst("^jdbc:", "");
    String username = pgConfig.getString("user", "postgres");
    String password = pgConfig.getString("password", "");
    int maxPoolSize = pgConfig.getInteger("pool-size", 10);

    // Extract primary database connection options
    PgConnectOptions primaryConnectOptions = PgConnectOptions.fromUri(primaryUrl)
      .setUser(username)
      .setPassword(password)
      //.addProperty("stringtype", "unspecified") // Query parameter
      .setSslMode(primaryUrl.contains("ssl=require") ? SslMode.REQUIRE : DEFAULT_SSLMODE);

    // Configure connection pool
    PoolOptions primaryPoolOptions = new PoolOptions()
      .setMaxSize(maxPoolSize);

    // Initialize primary pool
    primaryPool = Pool.pool(vertx, primaryConnectOptions, primaryPoolOptions);

    if (secondaryUrl.isEmpty()) {
      secondaryPool = primaryPool;
    } else {
      // Extract secondary database connection options
      PgConnectOptions secondaryConnectOptions = PgConnectOptions.fromUri(secondaryUrl)
        .setUser(username)
        .setPassword(password)
        //.addProperty("stringtype", "unspecified") // Query parameter
        .setSslMode(primaryConnectOptions.getSslMode());

      // Configure secondary pool
      PoolOptions secondaryPoolOptions = new PoolOptions()
        .setMaxSize(maxPoolSize);

      // Initialize secondary pool
      secondaryPool = Pool.pool(vertx, secondaryConnectOptions, secondaryPoolOptions);
    }
  }

  public <T> Future<RowSet<Row>> prepared(final String query, final Tuple values, SqlConnection connection) {
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
