package org.entcore.common.postgres;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.*;
import org.apache.commons.lang3.NotImplementedException;

import static org.entcore.common.postgres.PostgresClientBusHelper.*;

public class PostgresClientBusPublisher implements IPostgresClient {

    private static final Logger log = LoggerFactory.getLogger(PostgresClientBusPublisher.class);
    private final EventBus bus;
    private final Vertx vertx;
    private final JsonObject config;
    private final String suffix;

    public PostgresClientBusPublisher(final Vertx vertx, final JsonObject config, final String suffix) {
        this.bus = vertx.eventBus();
        this.vertx = vertx;
        this.config = config;
        this.suffix = suffix;
    }

    @Override
    public Future<RowSet<Row>> preparedQuery(final String query, final Tuple tuple) {
        final Promise<RowSet<Row>> promise = Promise.promise();
        final DeliveryOptions options = new DeliveryOptions().setLocalOnly(true);
        this.bus.request(getAddress(suffix), queryToJson(query, tuple), options, res -> {
            if(res.succeeded()){
                final JsonObject result = (JsonObject) res.result().body();
                promise.complete(resultToRowset(result));
            }else{
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<IPostgresTransaction> transaction() {
        final IPostgresTransaction transa =new PostgresClientBusHelper.PostgresTransactionBus(transaction -> {
            final Promise<Void> promise = Promise.promise();
            if(transaction.isCommit()) {
                final DeliveryOptions options = new DeliveryOptions().setLocalOnly(true);
                this.bus.request(getAddress(suffix), transactionToJson(transaction.getParams()), options, resBus -> {
                    if(resBus.succeeded()){
                        final JsonObject result = (JsonObject) resBus.result().body();
                        final JsonArray array = jsonToTransactionParams(result);
                        for(int i = 0 ; i < array.size(); i++){
                            final JsonObject res = array.getJsonObject(i);
                            if(isRowsetResult(res)){
                                final RowSet<Row> rowst = resultToRowset(res);
                                transaction.getPromises().get(i).complete(rowst);
                            }else if(isNotifyResult(res)){
                                transaction.getPromises().get(i).complete(null);
                            }else{
                                log.error("Could not parse transaction result: "+res);
                                transaction.getPromises().get(i).fail("Could not parse");
                            }
                        }
                        promise.complete(null);
                    }else{
                        promise.fail(resBus.cause());
                    }
                });
            } else {
                promise.fail("rollback");
            }
            return promise.future().mapEmpty();
        });
        return Future.succeededFuture(transa);
    }

    @Override
    public PostgresClientChannel getClientChannel() {
        final PgSubscriber pgSubscriber = PgSubscriber.subscriber(vertx, IPostgresClient.getConnectOption(config));
        return new PostgresClientChannel(pgSubscriber, config);
    }

    @Override
    public Future<RowStream<Row>> queryStream(String query, Tuple tuple, int batchSize) {
        throw new NotImplementedException("cannot query row stream using bus");
    }

}
