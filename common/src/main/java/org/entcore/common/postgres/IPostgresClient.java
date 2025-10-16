package org.entcore.common.postgres;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.*;

import java.util.function.Function;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

public interface IPostgresClient {
    Future<RowSet<Row>> preparedQuery(String query, Tuple tuple);

    <T> Future<@Nullable T>  transaction(Function<SqlConnection, Future<@Nullable T>> function);

    Future<RowStream<Row>> queryStream(String query, Tuple tuple, int batchSize);

    PostgresClientChannel getClientChannel();

    static Future<IPostgresClient> create(final Vertx vertx, final JsonObject config, final boolean worker, final boolean pool) {
      try {
        return getPostgresConfig(vertx, config)
          .map(postgresConfig -> {
            final PostgresClient baseClient = new PostgresClient(vertx, postgresConfig);
            return pool ? baseClient.getClientPool() : baseClient;
          });
      } catch (Exception e) {
        return failedFuture(e);
      }
    }

    static Future<JsonObject> getPostgresConfig(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("postgresConfig") != null) {
            final JsonObject postgresqlConfig = config.getJsonObject("postgresConfig");
            return succeededFuture(postgresqlConfig);
        } else {
            return vertx.sharedData().<String, String>getAsyncMap("server")
              .flatMap(m -> m.get("postgresConfig"))
              .flatMap(postgresConfig -> {
                final Future<JsonObject> pgConf;
                if (postgresConfig != null) {
                  pgConf = succeededFuture(new JsonObject(postgresConfig));
                } else {
                  pgConf = failedFuture("Missing postgresConfig config");
                }
                return pgConf;
              });
        }
    }


    static Future<PostgresClientChannel> createChannel(final Vertx vertx, final JsonObject config) throws Exception {
        return getPostgresConfig(vertx, config)
          .map(realConfig -> {
            final PgSubscriber pgSubscriber = PgSubscriber.subscriber(vertx, IPostgresClient.getConnectOption(realConfig));
            return new PostgresClientChannel(pgSubscriber, config);
          });
    }

    static PgConnectOptions getConnectOption(final JsonObject config){
        final SslMode sslMode = SslMode.valueOf(config.getString("ssl-mode", "DISABLE"));
        final PgConnectOptions options = new PgConnectOptions()
                .setPort(config.getInteger("port", 5432))
                .setHost(config.getString("host"))
                .setDatabase(config.getString("database"))
                .setUser(config.getString("user"))
                .setPassword(config.getString("password"));
        if (!SslMode.DISABLE.equals(sslMode)) {
            options.setSslMode(sslMode).setTrustAll(SslMode.ALLOW.equals(sslMode) || SslMode.PREFER.equals(sslMode) || SslMode.REQUIRE.equals(sslMode));
        }
        return options;
    }


    static Future<Void> notify(final SqlConnection connection, final String channel, final String message) {
        final Promise<Void> future = Promise.promise();
        //prepareQuery not works with notify allow only internal safe message
        connection.query(
            "NOTIFY " + channel + ", '" + message + "'").execute(notified -> {
            future.handle(notified.mapEmpty());
            if (notified.failed()) {
                future.fail(notified.cause());
            } else {
                future.complete();
            }
        });
        return future.future();
    }
}
