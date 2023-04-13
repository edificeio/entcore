package org.entcore.common.messaging.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.backends.BackendRegistries;
import static java.util.Collections.emptyList;
import org.entcore.common.messaging.IMessagingClientMetricsRecorder;
import static org.entcore.common.utils.MetricsUtils.getSla;
import static org.entcore.common.utils.MetricsUtils.setTimerSla;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MicrometersMessagingClientMetricsRecorder implements IMessagingClientMetricsRecorder {
    private final Timer messageSendingTimes;
    private final Counter sendSuccessCounter;
    private final Counter sendFailureCounter;
    private final Timer messageProcessingTimes;
    private final Timer messageLifetimeTimes;
    private final Counter processingSuccessCounter;
    private final Counter processingFailureCounter;

    public MicrometersMessagingClientMetricsRecorder(final Configuration configuration) {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry == null) {
            throw new IllegalStateException("micrometer.registries.empty");
        }
        final String[] tags = configuration.tags == null ? new String[]{} : configuration.tags.toArray(new String[]{});
        messageSendingTimes = setTimerSla(
            Timer.builder("messaging.sending.time")
                .tags(tags)
                .description("time to send messages"),
            configuration.sendSla, 500l
        ).tags(tags).register(registry);

        messageProcessingTimes = setTimerSla(
                Timer.builder("messaging.processing.time")
                        .tags(tags)
                        .description("time to process messages"),
                configuration.processingSla, 10000l
        ).tags(tags).register(registry);

        messageLifetimeTimes = setTimerSla(
                Timer.builder("messaging.lifetime.time")
                        .tags(tags)
                        .description("lifetime of messages"),
                configuration.lifetimeSla, 10000l
        ).tags(tags).register(registry);

        sendSuccessCounter = Counter.builder("message.send.ok")
                .description("number of times a message was successfully sent")
                .tags(tags)
                .register(registry);

        sendFailureCounter = Counter.builder("message.send.ko")
                .description("number of times a message failed to be sent")
                .tags(tags)
                .register(registry);

        processingFailureCounter = Counter.builder("message.process.ko")
                .description("number of times an error occurred while processing a message")
                .tags(tags)
                .register(registry);

        processingSuccessCounter = Counter.builder("message.process.ok")
                .description("number of times a message was successfully treated")
                .tags(tags)
                .register(registry);
    }

    @Override
    public void onSendMessageSuccess(final int numberOfSuccessfulMessages, final long duration) {
        sendSuccessCounter.increment(numberOfSuccessfulMessages);
        messageSendingTimes.record(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSendMessageFailure(final int numberOfFailedMessages, final long duration) {
        sendFailureCounter.increment(numberOfFailedMessages);
        messageSendingTimes.record(duration, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onMessageProcessSuccessfully(final long processingDuration, final long lifetime) {
        messageProcessingTimes.record(processingDuration, TimeUnit.MILLISECONDS);
        messageLifetimeTimes.record(lifetime, TimeUnit.MILLISECONDS);
        processingSuccessCounter.increment();
    }

    @Override
    public void onMessageProcessError(final long processingDuration, final long lifetime) {
        messageProcessingTimes.record(processingDuration, TimeUnit.MILLISECONDS);
        messageLifetimeTimes.record(lifetime, TimeUnit.MILLISECONDS);
        processingFailureCounter.increment();
    }

    public static class Configuration {
        private final List<Duration> sendSla;
        private final List<Duration> processingSla;
        private final List<Duration> lifetimeSla;
        private final List<String> tags;

        public Configuration(final List<Duration> sendSla,
                             final List<Duration> processingSla,
                             final List<Duration> lifetimeSla,
                             final List<String> tags) {
            this.sendSla = sendSla;
            this.processingSla = processingSla;
            this.lifetimeSla = lifetimeSla;
            this.tags = tags;
        }

        /**
         * Create the recorder's configuration from the attribute {@code metrics} of the parameter {@code conf} (or send
         * back a default configuration if conf is {@code null} or if it has no {@code metrics} field.
         * @param conf an object with the following keys :
         *          <ul>
         *               <li>sendSla, an ordered list of longs whose values are the bucket boundaries for the exported
         *               Timer which records the time to send messages to Redis</li>
         *               <li>processingSla, an ordered list of longs whose values are the bucket boundaries for the exported
         *               Timer which records the time to process messages</li>
         *               <li>lifetimeSla, an ordered list of longs whose values are the bucket boundaries for the exported
         *               Timer which records the time between the creation of a message and the end of its processing</li>
         *           </ul>
         * @return The built configuration
         */
        public static Configuration fromJson(final JsonObject conf) {
            final List<Duration> sendSla;
            final List<Duration> lifetimeSla;
            final List<Duration> processingSla;
            final List<String> tags;
            if(conf == null || !conf.containsKey("metrics")) {
                sendSla = emptyList();
                lifetimeSla = emptyList();
                processingSla = emptyList();
                tags = emptyList();
            } else {
                final JsonObject metrics = conf.getJsonObject("metrics");
                sendSla = getSla("sendSla", metrics);
                lifetimeSla = getSla("lifetimeSla", metrics);
                processingSla = getSla("processingSla", metrics);
                tags = metrics.getJsonArray("tags", new JsonArray()).getList();
            }
            return new Configuration(sendSla, processingSla, lifetimeSla, tags);
        }
    }

}
