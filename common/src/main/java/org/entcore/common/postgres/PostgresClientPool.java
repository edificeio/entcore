package org.entcore.common.postgres;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PostgresClientPool implements IPostgresClient {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientPool.class);
    private final PgPool pgPool;
    private final JsonObject config;
    private final Vertx vertx;

    PostgresClientPool(final Vertx vertx, final PgPool pgPool, final JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.pgPool = pgPool;
    }

    @Override
    public PostgresClientChannel getClientChannel() {
        final PgSubscriber pgSubscriber = PgSubscriber.subscriber(vertx, IPostgresClient.getConnectOption(config));
        return new PostgresClientChannel(pgSubscriber, config);
    }

    public Future<JsonObject> insert(final String table, final JsonObject json){
        final String query = PostgresClient.toInsertQuery(table, json);
        final Tuple tuple = PostgresClient.toInsertTuple(json);
        final Promise<JsonObject> future = Promise.promise();
        this.pgPool.preparedQuery(query).execute(tuple, e->{
            if(e.succeeded()){
                final RowSet<Row> rows = e.result();
                final Row row = rows.iterator().next();
                final JsonObject res = PostgresClient.toJson(row);
                future.complete(res);
            }else{
                future.fail(e.cause());
            }
        });
        return future.future();
    }

    public Future<JsonObject> update(final String table, final JsonObject json) {
        return update(table, json, "id");
    }

    public Future<JsonObject> update(final String table, final JsonObject json, final String idColumn){
        final String query = PostgresClient.toUpdateQuery(table, json, idColumn);
        final Tuple tuple = PostgresClient.toUpdateTuple(json, idColumn);
        final Promise<JsonObject> future = Promise.promise();
        this.pgPool.preparedQuery(query).execute(tuple, e->{
            if(e.succeeded()){
                final RowSet<Row> rows = e.result();
                final Row row = rows.iterator().next();
                final JsonObject res = PostgresClient.toJson(row);
                future.complete(res);
            }else{
                future.fail(e.cause());
            }
        });
        return future.future();
    }

    public Future<List<JsonObject>> update(final String table, final JsonObject json, final Set<Integer> ids) {
        return update(table, json, "id", ids);
    }

    public Future<List<JsonObject>> update(final String table, final JsonObject json, final String idColumn, final Set<Integer> ids){
        if(ids.isEmpty()){
            return Future.succeededFuture(new ArrayList<>());
        }
        final String query = PostgresClient.toUpdateQuery(table, json, idColumn, ids);
        final Tuple tuple = PostgresClient.toUpdateTuple(json, idColumn, ids);
        final Promise<List<JsonObject>> future = Promise.promise();
        this.pgPool.preparedQuery(query).execute(tuple, e->{
            if(e.succeeded()){
                final List<JsonObject> all = new ArrayList<>();
                for(final Row row : e.result()){
                    final JsonObject res = PostgresClient.toJson(row);
                    all.add(res);
                }
                future.complete(all);
            }else{
                future.fail(e.cause());
            }
        });
        return future.future();
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

    public Future<IPostgresTransaction> transaction() {
        final Future<IPostgresTransaction> future = Future.future();
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

    public Future<RowStream<Row>> queryStream(final String query, final Tuple tuple, final int batchSize) {
        final Promise<RowStream<Row>> future = Promise.promise();
        this.pgPool.begin(resConn -> {
            if(resConn.succeeded()){
                //need transaction https://github.com/eclipse-vertx/vertx-sql-client/issues/128
                final Transaction transaction = resConn.result();
                transaction.prepare(query, resPrepare -> {
                    if(resPrepare.succeeded()){
                        final PreparedStatement prepared = resPrepare.result();
                        final RowStream<Row> stream = prepared.createStream(batchSize,tuple);
                        future.complete(new RowStream<Row>() {
                            private boolean closed = false;
                            private void closeIfNeeded(){
                                if(closed) {
                                    log.debug("[PostgresClientPool@queryStream] Transaction already closed");
                                } else {
                                    log.debug("[PostgresClientPool@queryStream] Closing transaction...");
                                    transaction.rollback(e -> {
                                        if(e.succeeded()) {
                                            log.debug("[PostgresClientPool@queryStream] ... transaction closed !");
                                        } else {
                                            log.error("[PostgresClientPool@queryStream] ... could not close transaction", e.cause());
                                        }
                                    });
                                }
                                closed = true;
                            }
                            @Override
                            public RowStream<Row> exceptionHandler(Handler<Throwable> handler) {
                                stream.exceptionHandler(e -> {
                                    closeIfNeeded();
                                    handler.handle(e);
                                });
                                return this;
                            }

                            @Override
                            public RowStream<Row> handler(Handler<Row> handler) {
                                stream.handler(handler);
                                return this;
                            }

                            @Override
                            public RowStream<Row> pause() {
                                stream.pause();
                                return this;
                            }

                            @Override
                            public RowStream<Row> resume() {
                                stream.resume();
                                return this;
                            }

                            @Override
                            public RowStream<Row> endHandler(Handler<Void> endHandler) {
                                stream.endHandler(e -> {
                                    closeIfNeeded();
                                    endHandler.handle(e);
                                });
                                return this;
                            }

                            @Override
                            public void close() {
                                stream.close();
                                closeIfNeeded();
                            }

                            @Override
                            public void close(Handler<AsyncResult<Void>> completionHandler) {
                                stream.close(completionHandler);
                                closeIfNeeded();
                            }

                            @Override
                            public ReadStream<Row> fetch(long amount) {
                                return stream.fetch(amount);
                            }
                        });
                    }else{
                        future.fail(resPrepare.cause());
                    }
                });
            }else{
                future.fail(resConn.cause());
            }
        });
        return future.future();
    }
}
