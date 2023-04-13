package org.entcore.common.messaging.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import org.entcore.common.messaging.AppMessageProcessor;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.to.ClientMessage;
import org.entcore.common.redis.RedisClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RedisMessagingClient implements IMessagingClient {

    public static final Logger log = LoggerFactory.getLogger(RedisMessagingClient.class);

    private final Vertx vertx;
    private final RedisClient redisClient;
    private final String stream;
    private final String consumerGroup;
    private final String consumerName;
    private final int consumerBlockMs;
    private boolean listening = false;

    public RedisMessagingClient(
            final Vertx vertx, final RedisClient redisClient,
            final String stream,
            final String consumerGroup,
            final String consumerName,
            final int consumerBlockMs) {
        this.vertx = vertx;
        this.redisClient = redisClient;
        this.stream = stream;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
        this.consumerBlockMs = consumerBlockMs;
    }

    @Override
    public Future<List<String>> pushMessages(final Object... messages) {
        final List<JsonObject> serializedMessages = Arrays.stream(messages)
            .map(JsonObject::mapFrom)
            .collect(Collectors.toList());
        return redisClient.xAdd(stream, serializedMessages);
    }

    @Override
    public String getAddress() {
        return stream;
    }

    @Override
    public <T extends ClientMessage> Future<Void> startListening(final AppMessageProcessor<T> handler) {
        final Future<Void> onStarted;
        if(canListen()) {
            final List<String> streams = new ArrayList<>();
            streams.add(stream);
            onStarted = redisClient.xcreateGroup(consumerGroup, streams)
                    .onSuccess(e -> {
                        listening = true;
                        doListen(handler);
                    })
                    .onFailure(th -> listening = false);
        } else {
            onStarted = Future.failedFuture("missing.consumer.information.for.listening");
        }
        return onStarted;
    }

    private <T extends ClientMessage> void doListen(final AppMessageProcessor<T> handler) {
        if(listening) {
            redisClient.xreadGroup(consumerGroup,consumerName, singletonList(stream), true,
                    Optional.of(1),
                    Optional.of(consumerBlockMs),
                    Optional.of(">"),
                    true).onComplete(res -> {
                if(res.failed()) {
                    log.error("Could not read xstream", res.cause());
                } else {
                    final List<Future> handlesOnPayloads = res.result().stream().map(message -> {
                        final T payload = message.mapTo(handler.getHandledMessageClass());
                        return handler.apply(payload);
                    }).collect(Collectors.toList());
                    CompositeFuture.all(handlesOnPayloads).onComplete(e -> doListen(handler));
                }
            });
        }
    }

    @Override
    public Future<Void> stopListening() {
        listening = false;
        return Future.succeededFuture();
    }

    @Override
    public boolean canListen() {
        return isNotEmpty(consumerGroup) && isNotEmpty(consumerName);
    }

    @Override
    public boolean isListening() {
        return listening;
    }
}
