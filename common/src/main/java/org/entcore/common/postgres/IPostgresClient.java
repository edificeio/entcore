package org.entcore.common.postgres;

import fr.wseduc.webutils.security.Md5;
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

public interface IPostgresClient {
    Future<RowSet<Row>> preparedQuery(String query, Tuple tuple);

    <T> Future<@Nullable T>  transaction(Function<SqlConnection, Future<@Nullable T>> function);

    Future<RowStream<Row>> queryStream(String query, Tuple tuple, int batchSize);

    PostgresClientChannel getClientChannel();

    static IPostgresClient create(final Vertx vertx, final JsonObject config, final boolean worker, final boolean pool) throws Exception{
        final JsonObject postgresConfig = getPostgresConfig(vertx, config);
        final PostgresClient baseClient = new PostgresClient(vertx, postgresConfig);
        final IPostgresClient postgresClient = pool? baseClient.getClientPool(): baseClient;
        return postgresClient;
    }

    static JsonObject getPostgresConfig(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("postgresConfig") != null) {
            final JsonObject postgresqlConfig = config.getJsonObject("postgresConfig");
            return postgresqlConfig;
        }else{
            final String postgresConfig = (String) vertx.sharedData().getLocalMap("server").get("postgresConfig");
            if(postgresConfig!=null){
                return new JsonObject(postgresConfig);
            }else{
                throw new Exception("Missing postgresConfig config");
            }
        }
    }


    static PostgresClientChannel createChannel(final Vertx vertx, final JsonObject config) throws Exception {
        final JsonObject realConfig = getPostgresConfig(vertx, config);
        final PgSubscriber pgSubscriber = PgSubscriber.subscriber(vertx, IPostgresClient.getConnectOption(realConfig));
        return new PostgresClientChannel(pgSubscriber, config);
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
