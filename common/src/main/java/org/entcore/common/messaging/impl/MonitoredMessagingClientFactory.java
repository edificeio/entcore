package org.entcore.common.messaging.impl;

import io.vertx.core.Vertx;
import org.entcore.common.messaging.IMessagingClient;
import org.entcore.common.messaging.IMessagingClientFactory;
import org.entcore.common.messaging.IMessagingClientMetricsRecorder;

/**
 * A {@code MessaginClientFactory} wrapper to return {@code IMessagingClient} which monitors their exchanges.
 */
public class MonitoredMessagingClientFactory implements IMessagingClientFactory {
    private final IMessagingClientFactory factory;
    private final IMessagingClientMetricsRecorder metricsRecorder;
    public static final String METRICS_OPTIONS_NAME = "metrics";

    /**
     * @param factory The factory to wrap
     * @param vertx Vertx instance
     * @param metricsConfig Metrics configuration
     */
    public MonitoredMessagingClientFactory(final IMessagingClientFactory factory, final Vertx vertx,
                                           final MicrometersMessagingClientMetricsRecorder.Configuration metricsConfig) {
        this.factory = factory;
        metricsRecorder = new MicrometersMessagingClientMetricsRecorder(metricsConfig);
    }

    @Override
    public IMessagingClient create() {
        return new MessagingClientWithMetrics(factory.create(), metricsRecorder);
    }
}
