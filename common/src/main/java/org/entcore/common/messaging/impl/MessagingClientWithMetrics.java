package org.entcore.common.messaging.impl;

import io.vertx.core.Future;
import org.entcore.common.messaging.AppMessageProcessor;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.IMessagingClientMetricsRecorder;
import org.entcore.common.messaging.to.ClientMessage;

import java.util.List;

/**
 * Wrapper around another client to measure its usage.
 */
public class MessagingClientWithMetrics implements IMessagingClient {
    private final IMessagingClient client;
    private final IMessagingClientMetricsRecorder metricsRecorder;

    public MessagingClientWithMetrics(final IMessagingClient client,
                                      final IMessagingClientMetricsRecorder metricsRecorder) {
        this.client = client;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public Future<List<String>> pushMessages(final Object... messages) {
        final long start = System.currentTimeMillis();
        return client.pushMessages(messages).onSuccess(sent ->
            metricsRecorder.onSendMessageSuccess(sent.size(), System.currentTimeMillis() - start)
        ).onFailure(th ->
            metricsRecorder.onSendMessageFailure(messages.length, System.currentTimeMillis() - start)
        );
    }

    @Override
    public String getAddress() {
        return client.getAddress();
    }

    @Override
    public <T extends ClientMessage> Future<Void> startListening(final AppMessageProcessor<T> handler) {
        return client.startListening(new AppMessageProcessor<T>() {
            @Override
            public Class<T> getHandledMessageClass() {
                return handler.getHandledMessageClass();
            }

            @Override
            public Future apply(final T message) {
                final long start = System.currentTimeMillis();
                return ((Future<?>)handler.apply(message)).onComplete(processResult -> {
                    final long now = System.currentTimeMillis();
                    final long processingDuration = now - start;
                    final long lifetimeDuration = now - message.getCreationTime();
                    if(processResult.succeeded()) {
                        metricsRecorder.onMessageProcessSuccessfully(processingDuration, lifetimeDuration);
                    } else {
                        metricsRecorder.onMessageProcessError(processingDuration, lifetimeDuration);
                    }
                });
            }
        });
    }

    @Override
    public Future<Void> stopListening() {
        return client.stopListening();
    }

    @Override
    public boolean canListen() {
        return client.canListen();
    }

    @Override
    public boolean isListening() {
        return client.isListening();
    }
}
