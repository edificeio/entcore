package org.entcore.common.postgres;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;
import static org.entcore.common.postgres.PostgresClientBusHelper.*;

public class PostgresClientBusConsumer implements IPostgresClient {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientBusConsumer.class);
    private final IPostgresClient pgClient;
    private final MessageConsumer<JsonObject> consumer;
    private static PostgresClientBusConsumer instance;

    public static PostgresClientBusConsumer initInstance(final Vertx vertx, final IPostgresClient inner, final String suffix){
        if(instance == null){
            instance =  new PostgresClientBusConsumer(vertx, inner, suffix);
        }
        return instance;
    }

    PostgresClientBusConsumer(final Vertx vertx, final IPostgresClient inner, final String suffix){
        this.pgClient = inner;
        this.consumer = vertx.eventBus().localConsumer(PostgresClientBusHelper.getAddress(suffix));
        this.consumer.handler(message -> {
            try {
                final JsonObject payload = message.body();
                if (isQuery(payload)) {
                    final String query = jsonToQuery(payload);
                    final Tuple tuple = jsonToQueryTuple(payload);
                    this.preparedQuery(query, tuple).onComplete(res -> {
                        if (res.succeeded()) {
                            final JsonObject result = resultToJson(res.result());
                            message.reply(result);
                        } else {
                            message.fail(500, res.cause().getMessage());
                            log.error("Query failed: ", res.cause());
                        }
                    });
                } else if (isTransaction(payload)) {
                    this.transaction().onComplete(res -> {
                        if (res.succeeded()) {
                            final IPostgresTransaction transaction = res.result();
                            final JsonArray params = jsonToTransactionParams(payload);
                            final List<JsonObject> results = new ArrayList<>(params.size());
                            final List<Future> futureResults = new ArrayList<>();
                            for (int i = 0; i < params.size(); i++) {
                                final JsonObject param = params.getJsonObject(i);
                                if (isQuery(param)) {
                                    final String query = jsonToQuery(param);
                                    final Tuple tuple = jsonToQueryTuple(param);
                                    final int index = i;
                                    futureResults.add(transaction.addPreparedQuery(query, tuple).onComplete(queryRes -> {
                                        if (queryRes.succeeded()) {
                                            final JsonObject result = resultToJson(queryRes.result());
                                            results.add(index, result);
                                        } else {
                                            log.error("Query failed: " + query, queryRes.cause());
                                        }
                                    }));
                                } else if (isNotify(param)) {
                                    final String channel = jsonToNotifyChannel(param);
                                    final String mess = jsonToNotifyMessage(param);
                                    final int index = i;
                                    futureResults.add(transaction.notify(channel, mess).onComplete(notifyRes -> {
                                        if (notifyRes.succeeded()) {
                                            final JsonObject result = resultNotifyToJson();
                                            results.add(index, result);
                                        }
                                    }));
                                } else {
                                    futureResults.add(Future.succeededFuture());
                                    log.warn("Transaction parsed query failed: ", param);
                                }
                            }
                            transaction.commit().onComplete(resCommit -> {
                                if (resCommit.succeeded()) {
                                    //wait until all finish
                                    CompositeFuture.all(futureResults).onSuccess(e -> {
                                        final JsonObject jsonTr = transactionToJson(new JsonArray(results));
                                        message.reply(jsonTr);
                                    }).onFailure(e -> {
                                        log.error("Transaction commit failed: ", e);
                                    });
                                } else {
                                    message.fail(500, resCommit.cause().getMessage());
                                    log.error("Transaction commit failed: ", resCommit.cause());
                                }
                            });
                        } else {
                            message.fail(500, res.cause().getMessage());
                            log.error("Transaction failed: ", res.cause());
                        }
                    });
                } else {
                    log.error("Could not parse query: " + payload);
                    message.fail(500, "parse failed");
                }
            }catch(final Exception e){
                log.error("Could not exec query: ", e);
                message.fail(500, e.getMessage());
            }
        });
    }

    public void stop(){
        this.consumer.unregister();
    }

    @Override
    public PostgresClientChannel getClientChannel() {
        return pgClient.getClientChannel();
    }

    @Override
    public Future<RowStream<Row>> queryStream(String query, Tuple tuple, int batchSize) {
        return pgClient.queryStream(query, tuple, batchSize);
    }

    @Override
    public Future<RowSet<Row>> preparedQuery(String query, Tuple tuple) {
        return pgClient.preparedQuery(query, tuple);
    }

    @Override
    public Future<IPostgresTransaction> transaction() {
        return pgClient.transaction();
    }
}
