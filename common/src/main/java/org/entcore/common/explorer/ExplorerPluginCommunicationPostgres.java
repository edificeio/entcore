package org.entcore.common.explorer;

import io.reactiverse.pgclient.Tuple;
import io.reactiverse.pgclient.data.Json;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.postgres.PostgresClientPool;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ExplorerPluginCommunicationPostgres implements IExplorerPluginCommunication {
    public static final String RESOURCE_CHANNEL = "channel_resource";
    static Logger log = LoggerFactory.getLogger(ExplorerPluginCommunicationPostgres.class);
    private final PostgresClientPool pgPool;
    private final List<PostgresExplorerFailed> pendingFailed = new ArrayList<>();
    private final List<Promise> pending = new ArrayList<>();
    private final Vertx vertx;
    private final int retryUntil = 30000;

    public ExplorerPluginCommunicationPostgres(final Vertx vertx, final PostgresClient pgClient) {
        this.pgPool = pgClient.getClientPool();
        this.vertx = vertx;
    }

    @Override
    public Future<Void> pushMessage(final ExplorerMessage message) {
        return pushMessage(Arrays.asList(message));
    }

    @Override
    public Future<Void> pushMessage(final List<ExplorerMessage> messages) {
        if (messages.isEmpty()) {
            return Future.succeededFuture();
        }
        final Promise promise = Promise.promise();
        pending.add(promise);
        return pgPool.transaction().compose(transaction -> {
            final LocalDateTime now = LocalDateTime.now();
            final List<Map<String, Object>> rows = messages.stream().map(e -> {
                final Map<String, Object> map = new HashMap<>();
                map.put("id_resource", e.getId());
                map.put("created_at", now);
                map.put("resource_action", e.getAction());
                map.put("payload", Json.create(e.getMessage()));
                map.put("priority", e.getPriority().getValue());
                return map;
            }).collect(Collectors.toList());
            final String placeholder = PostgresClient.insertPlaceholdersFromMap(rows, 1, "id_resource", "created_at", "resource_action", "payload", "priority");
            final Tuple values = PostgresClient.insertValuesFromMap(rows, Tuple.tuple(), "id_resource", "created_at", "resource_action", "payload", "priority");
            //TODO dynamic table name?
            final String query = String.format("INSERT INTO explorer.resource_queue (id_resource,created_at, resource_action, payload, priority) VALUES %s", placeholder);
            transaction.addPreparedQuery(query, values).onComplete(r -> {
                if (r.failed()) {
                    //TODO push somewhere else to retry? limit in size? in time? fallback to redis?
                    final PostgresExplorerFailed fail = new PostgresExplorerFailed(query, values);
                    pendingFailed.add(fail);
                    vertx.setTimer(retryUntil, rr -> {
                        pendingFailed.remove(fail);
                    });
                    log.error("Failed to push resources to queue: ", r.cause());
                    log.error("Query causing error: " + query);
                }
            });
            //retry failed
            for (final PostgresExplorerFailed failed : pendingFailed) {
                transaction.addPreparedQuery(failed.query, failed.tuple).onComplete(r -> {
                    if (r.succeeded()) {
                        pendingFailed.remove(failed);
                    }
                });
            }
            //
            transaction.notify(RESOURCE_CHANNEL, "new_resources").onComplete(e -> {
                if (e.failed()) {
                    log.error("Failed to notify new ressources: ", e.cause());
                }
            });
            final Promise<Void> future = Promise.promise();
            transaction.commit().onComplete(e -> {
                future.handle(e);
                if (e.failed()) {
                    log.error("Failed to commit resources to queue: ", e.cause());
                }
            });
            return future.future();
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
