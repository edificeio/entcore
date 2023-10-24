package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PostgresClientChannel {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientChannel.class);
    private final PgSubscriber pgSubscriber;
    private Promise<Void> onConnect;

    PostgresClientChannel(final PgSubscriber aPgSubscriber, final JsonObject config) {
        this.pgSubscriber = aPgSubscriber;
        //reconnection policy
        final int reconnectCount = config.getInteger("reconnect-count", 10);
        final long reconnectDelay = config.getLong("reconnect-delay-ms", 200L);
        pgSubscriber.reconnectPolicy(retries -> {
            if (retries < reconnectCount) {
                log.error("Trying to reconnect to the server... " + retries);
                return reconnectDelay;
            } else {
                //disconnect
                log.error("Could not reconnect to the server");
                onConnect = null;
                return -1L;
            }
        });
        //connect
        ensureConnect();
    }

    private Future<Void> ensureConnect() {
        if (onConnect == null) {
            onConnect = Promise.promise();
            this.pgSubscriber.connect(res -> {
                if (res.failed()) {
                    log.error("Could not connect to server");
                }
                onConnect.handle(res);
            });
        }
        return onConnect.future();
    }

    public Future<Void> notify(final String channel, final String message) {
        return this.ensureConnect().compose(resConnection -> {
            final Promise<Void> future = Promise.promise();
            this.pgSubscriber.actualConnection().query(
                    "NOTIFY " + channel + ", '" + message + "'").execute(notified -> {
                        if (notified.failed()) {
                            log.error("Could not notify channel: " + channel);
                        }
                        future.handle(notified.mapEmpty());
                    });
            return future.future();
        });
    }

    public void listen(String channel, final Handler<String> handler) {
        this.pgSubscriber.channel(channel).handler(handler);
    }

    public Future<PostgresClient.PostgresTransaction> transaction() {
        return this.ensureConnect()
            .flatMap(r -> this.pgSubscriber.actualConnection().begin())
            .map(t -> new PostgresClient.PostgresTransaction(this.pgSubscriber.actualConnection()));
    }

    public Future<RowSet<Row>> preparedQuery(String query, Tuple tuple) {
        return this.ensureConnect()
            .flatMap(r -> this.pgSubscriber.actualConnection().preparedQuery(query).execute(tuple));
    }

    public void close() {
        pgSubscriber.close();
        onConnect = null;
    }
}
