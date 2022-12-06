package org.entcore.common.explorer.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
import org.entcore.common.explorer.IExplorerPluginMetricsRecorder;

/**
 * Records (via micrometer) metrics coming from the activity in the explorer plugin.
 */
public class MicrometerExplorerPluginMetricsRecorder implements IExplorerPluginMetricsRecorder {
    private final Counter successfullySentMessagesCounter;
    private final Counter failedSendMessagesCounter;

    public MicrometerExplorerPluginMetricsRecorder() {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry == null) {
            throw new IllegalStateException("micrometer.registries.empty");
        }
        successfullySentMessagesCounter = Counter.builder("explorer.plugin.messages.sent.success")
                .description("number of messages successfully sent by the plugin")
                .register(registry);
        failedSendMessagesCounter = Counter.builder("explorer.plugin.messages.sent.failure")
                .description("number of messages that could not be sent by the plugin")
                .register(registry);
    }

    @Override
    public void onSendMessageSuccess(int numberOfSuccessfulMessages) {
        successfullySentMessagesCounter.increment(numberOfSuccessfulMessages);
    }

    @Override
    public void onSendMessageFailure(int numberOfFailedMessages) {
        failedSendMessagesCounter.increment();
    }
}
