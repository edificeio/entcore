package org.entcore.common.explorer.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.explorer.ExplorerMessage;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IExplorerPluginMetricsRecorder;
import org.entcore.common.redis.RedisClient;

import java.util.*;
import java.util.stream.Collectors;

public class ExplorerPluginCommunicationRedis implements IExplorerPluginCommunication {
    public static final JsonArray DEFAULT_STREAMS = new JsonArray().add("explorer_high").add("explorer_medium").add("explorer_low");
    static Logger log = LoggerFactory.getLogger(ExplorerPluginCommunicationRedis.class);
    static Map<ExplorerMessage.ExplorerPriority, String> STREAMS = new HashMap<>();
    private final List<Promise> pending = new ArrayList<>();
    private final IExplorerPluginMetricsRecorder metricsRecorder;

    private static final List<String> STREAM_NAMES_ORDERED_BY_PRIO_DESC = Arrays.asList(
            "explorer_high",
            "explorer_medium",
            "explorer_low"
    );

    static {
        STREAMS.put(ExplorerMessage.ExplorerPriority.Low, "explorer_low");
        STREAMS.put(ExplorerMessage.ExplorerPriority.Medium, "explorer_medium");
        STREAMS.put(ExplorerMessage.ExplorerPriority.High, "explorer_high");
    }

    private final RedisClient redisClient;
    private final List<RedisExplorerFailed> pendingFailed = new ArrayList<>();
    private final Vertx vertx;
    private final int retryUntil = 30000;
    private boolean isEnabled = true;

    public ExplorerPluginCommunicationRedis(final Vertx vertx, final RedisClient redisClient,
                                            final IExplorerPluginMetricsRecorder metricsRecorder) {
        this.redisClient = redisClient;
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
        final List<Future> futures = new ArrayList<>();
        Future<List<String>> previous = Future.succeededFuture();
        final Map<String, List<JsonObject>> map = toRedisMap(messages);
        // Send messages by descending priority of streams
        // This works without versionning just because for now one type of resources is bound
        // to exactly one type of stream. As soon as a resource can be in two streams it needs to have an explicit
        // version field
        for (final String stream : STREAM_NAMES_ORDERED_BY_PRIO_DESC) {
            if(map.containsKey(stream)) {
                final List<JsonObject> messagesToSend = map.get(stream);
                final Future<List<String>> tmp = previous.compose(ee -> redisClient.xAdd(stream, messagesToSend).onFailure(e -> {
                    this.metricsRecorder.onSendMessageFailure(messagesToSend.size());
                    final RedisExplorerFailed fail = new RedisExplorerFailed(stream, map.get(stream));
                    pendingFailed.add(fail);
                    vertx.setTimer(retryUntil, rr -> {
                        pendingFailed.remove(fail);
                    });
                    log.error("Failed to push resources to stream " + stream, e);
                })).onSuccess(sentMessages -> {
                    this.metricsRecorder.onSendMessageSuccess(sentMessages.size());
                });
                previous = tmp;
                futures.add(tmp);
            }
        }
        final Promise promise = Promise.promise();
        pending.add(promise);
        return (Future)CompositeFuture.all(futures).onComplete(e->{
            pending.remove(promise);
            promise.complete();
        })
        .mapEmpty()
        .otherwiseEmpty();
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

    protected JsonObject toRedisJson(final ExplorerMessage message) {
        final JsonObject json = new JsonObject();
        json.put("resource_action", message.getAction());
        json.put("id_resource", message.getId());
        json.put("payload", message.getMessage().encode());
        return json;
    }

    protected Map<String, List<JsonObject>> toRedisMap(final List<ExplorerMessage> messages) {
        final Map<String, List<JsonObject>> map = new HashMap<>();
        for (final ExplorerMessage m : messages) {
            final String stream = STREAMS.get(m.getPriority());
            map.compute(stream, (k, listOfMessages) -> {
                final List<JsonObject> augmentedListOfMessages;
                if(listOfMessages == null) {
                    augmentedListOfMessages = new ArrayList<>();
                } else {
                    augmentedListOfMessages = listOfMessages;
                }
                augmentedListOfMessages.add(toRedisJson(m));
                return augmentedListOfMessages;
            });
        }
        return map;
    }

    class RedisExplorerFailed {
        final String stream;
        final List<JsonObject> jsons;

        public RedisExplorerFailed(final String stream, final List<JsonObject> jsons) {
            this.stream = stream;
            this.jsons = jsons;
        }
    }

}
