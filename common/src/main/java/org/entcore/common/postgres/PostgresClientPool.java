package org.entcore.common.postgres;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PostgresClientPool {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientPool.class);
    private final PgPool pgPool;
    private Future<Void> onReady;

    PostgresClientPool(final PgPool pgPool, final JsonObject config) {
        this.pgPool = pgPool;
    }

    public Future<Void> notify(final String channel, final String message) {
        final Future<Void> future = Future.future();
        this.pgPool.query(
                "NOTIFY " + channel + ", '" + message + "'").execute(notified -> {
                    if (notified.failed()) {
                        log.error("Could not notify channel: " + channel);
                    }
                    future.handle(notified.mapEmpty());
                });
        return future;
    }

    public Future<PostgresClient.PostgresTransaction> transaction() {
        final Future<PostgresClient.PostgresTransaction> future = Future.future();
        this.pgPool.begin(r -> {
            if (r.succeeded()) {
                future.complete(new PostgresClient.PostgresTransaction(r.result()));
            } else {
                future.fail(r.cause());
            }
        });
        return future;
    }

    public Future<RowSet<Row>> preparedQuery(final String query, final Tuple tuple) {
        final Future<RowSet<Row>> future = Future.future();
        this.pgPool.preparedQuery(query).execute(tuple, future.completer());
        return future;
    }
}
