package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;

public class PostgresClientChannel {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientChannel.class);
    private final PgSubscriber pgSubscriber;
    private Future<Void> onConnect;

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
            onConnect = Future.future();
            this.pgSubscriber.connect(res -> {
                if (res.failed()) {
                    log.error("Could not connect to server");
                }
                onConnect.handle(res);
            });
        }
        return onConnect;
    }

    public Future<Void> notify(final String channel, final String message) {
        return this.ensureConnect().compose(resConnection -> {
            final Future<Void> future = Future.future();
            this.pgSubscriber.actualConnection().query(
                    "NOTIFY " + channel + ", '" + message + "'").execute(notified -> {
                        if (notified.failed()) {
                            log.error("Could not notify channel: " + channel);
                        }
                        future.handle(notified.mapEmpty());
                    });
            return future;
        });
    }

    public void listen(String channel, final Handler<String> handler) {
        this.pgSubscriber.channel(channel).handler(handler);
    }

    public Future<PostgresClient.PostgresTransaction> transaction() {
        return this.ensureConnect().map(r -> {
            final Transaction pg = this.pgSubscriber.actualConnection().begin();
            return new PostgresClient.PostgresTransaction(pg);
        });
    }

    public Future<RowSet<Row>> preparedQuery(String query, Tuple tuple) {
        return this.ensureConnect().compose(r -> {
            final Future<RowSet<Row>> future = Future.future();
            this.pgSubscriber.actualConnection().preparedQuery(query).execute(tuple, future.completer());
            return future;
        });
    }

    public void close() {
        pgSubscriber.close();
        onConnect = null;
    }
}
