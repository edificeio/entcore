package org.entcore.common.explorer.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.explorer.ExplorerMessage;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IExplorerPluginMetricsRecorder;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.postgres.PostgresClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ExplorerPluginCommunicationPostgres implements IExplorerPluginCommunication {
    public static final String RESOURCE_CHANNEL = "channel_resource";
    static Logger log = LoggerFactory.getLogger(ExplorerPluginCommunicationPostgres.class);
    private final IPostgresClient pgPool;
    private final List<PostgresExplorerFailed> pendingFailed = new ArrayList<>();
    private final List<Promise> pending = new ArrayList<>();
    private final Vertx vertx;
    private final int retryUntil = 30000;
    private boolean isEnabled = true;
    private final IExplorerPluginMetricsRecorder metricsRecorder;

    public ExplorerPluginCommunicationPostgres(final Vertx vertx, final IPostgresClient pgClient,
                                               final IExplorerPluginMetricsRecorder metricsRecorder) {
        this.pgPool = pgClient;
        this.vertx = vertx;
        this.metricsRecorder = metricsRecorder;
    }

    public IExplorerPluginCommunication setEnabled(boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    @Override
    public Future<Void> pushMessage(final ExplorerMessage message) {
        return pushMessage(Arrays.asList(message));
    }

    @Override
    public Future<Void> pushMessage(final List<ExplorerMessage> messages) {
        if(!this.isEnabled){
            return Future.succeededFuture();
        }
        if (messages.isEmpty()) {
            return Future.succeededFuture();
        }
        final Promise promise = Promise.promise();
        pending.add(promise);
        return pgPool.transaction(sqlConnection -> {
            final List<Future<?>> futures = new ArrayList<>();
            final LocalDateTime now = LocalDateTime.now();
            final List<Map<String, Object>> rows = messages.stream().map(e -> {
                final Map<String, Object> map = new HashMap<>();
                map.put("id_resource", e.getId());
                map.put("created_at", now);
                map.put("resource_action", e.getAction());
                map.put("payload", e.getMessage());
                map.put("priority", e.getPriority().getValue());
                return map;
            }).collect(Collectors.toList());
            final String placeholder = PostgresClient.insertPlaceholdersFromMap(rows, 1, "id_resource", "created_at", "resource_action", "payload", "priority");
            final Tuple values = PostgresClient.insertValuesFromMap(rows, Tuple.tuple(), "id_resource", "created_at", "resource_action", "payload", "priority");
            //TODO dynamic table name?
            final String query = String.format("INSERT INTO explorer.resource_queue (id_resource,created_at, resource_action, payload, priority) VALUES %s", placeholder);
            final Promise<Void> promiseInsertion = Promise.promise();
            sqlConnection.preparedQuery(query).execute(values).onComplete(r -> {
                promiseInsertion.complete();
                if (r.failed()) {
                    this.metricsRecorder.onSendMessageFailure(messages.size());
                    //TODO push somewhere else to retry? limit in size? in time? fallback to redis?
                    final PostgresExplorerFailed fail = new PostgresExplorerFailed(query, values);
                    pendingFailed.add(fail);
                    vertx.setTimer(retryUntil, rr -> {
                        pendingFailed.remove(fail);
                    });
                    log.error("Failed to push resources to queue: ", r.cause());
                    log.error("Query causing error: " + query);
                } else {
                    this.metricsRecorder.onSendMessageSuccess(messages.size());
                }
            });
            futures.add(promiseInsertion.future());
            //retry failed
            for (final PostgresExplorerFailed failed : pendingFailed) {
                final Promise<Void> promiseFailure = Promise.promise();
                sqlConnection.preparedQuery(failed.query).execute(failed.tuple).onComplete(r -> {
                    promiseFailure.complete();
                    if (r.succeeded()) {
                        pendingFailed.remove(failed);
                        this.metricsRecorder.onSendMessageSuccess(failed.tuple.size());
                    }
                });
                futures.add(promiseFailure.future());
            }
            //
            final Promise<Void> promiseNotification = Promise.promise();
            IPostgresClient.notify(sqlConnection, RESOURCE_CHANNEL, "new_resources").onComplete(e -> {
                promiseNotification.complete();
                if (e.failed()) {
                    log.error("Failed to notify new ressources: ", e.cause());
                }
            });
            futures.add(promiseNotification.future());
            return (Future)Future.join(futures).mapEmpty();
        }).onComplete(e->{
            promise.complete();
            pending.remove(promise);
        });
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }

    @Override
    public Future<Void> waitPending() {
        final List<Future> futures = pending.stream().map(e->e.future()).collect(Collectors.toList());
        return CompositeFuture.all(futures).mapEmpty();
    }

    class PostgresExplorerFailed {
        final String query;
        final Tuple tuple;

        public PostgresExplorerFailed(String query, Tuple tuple) {
            this.query = query;
            this.tuple = tuple;
        }
    }
}
