package org.entcore.common.messaging;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.messaging.impl.MicrometersMessagingClientMetricsRecorder;
import org.entcore.common.messaging.impl.MonitoredMessaginClientFactory;
import org.entcore.common.messaging.impl.NoopMessagingClientFactory;

/**
 * <p>
 *     <h4>Overview</h4>
 * Provides {@code IMessagingClientFactory} based on the configuration of the client.
 *</p>
 * <p>
 *     <h4>Configuration</h4>
 * It expects a configuration of this type :
 * <pre>{@code
 * {
 *     "type": "redis/noop/other",
 *     "metrics":
 *      {
 *          "sla": [],
 *          "tags": []
 *      }
 *     ...
 * }
 * }</pre>
 * It will base the creation of the factory on the value of the field {@code type} and then init the factory with
 * the same piece of configuration.
 *</p>
 * <p>
 *     <h4>NB</h4>
 * You should call {@code init(vertx)} before trying to get factory.
 * </p>
 * <p>
 *     <h4>Usage</h4>
 *     <pre>{@code
final JsonObject messagingConfig = serviceConfig.getJsonObject("messaging");
MessagingClientFactoryProvider.init(vertx);
this.messagingClientFactory = MessagingClientFactoryProvider.getFactory(messagingConfig);
 *     }</pre>
 * </p>
 */
public class MessagingClientFactoryProvider {
    private static Vertx vertx;
    private MessagingClientFactoryProvider() {}

    /**
     * Initialize the provider with global variables.
     * <br/>
     * <strong>Required before using other functions</strong>
     * @param vertx Vertx instance of the service in which the verticle who wants to communicate is running
     * @param config Portion of the configuration that contains messagingConfiguration.
     */
    public static void init(final Vertx vertx) {
        MessagingClientFactoryProvider.vertx = vertx;
    }

    /**
     * @param messagingConfig The configuration of our messaging clients
     * @return A factory to create messaging client.
     */
    public static IMessagingClientFactory getFactory(final JsonObject messagingConfig) {
        if(vertx == null ) {
            throw new IllegalArgumentException("messaging.client.factory.provider.missing.vertx");
        }
        final IMessagingClientFactory factory;
        if(messagingConfig == null) {
            factory = NoopMessagingClientFactory.instance;
        } else {
            final String type = messagingConfig.getString("type", "");
            final IMessagingClientFactory innerFactory;
            switch (type) {
                case "redis":
                    innerFactory = new RedisStreamClientFactory(vertx, messagingConfig);
                    break;
                default:
                    innerFactory = NoopMessagingClientFactory.instance;
            }
            final JsonObject metricsOptions = messagingConfig.getJsonObject("metrics", new JsonObject());
            if(metricsOptions.getBoolean("enabled", false)) {
                factory = new MonitoredMessaginClientFactory(innerFactory, vertx,
                        MicrometersMessagingClientMetricsRecorder.Configuration.fromJson(metricsOptions));
            } else {
                factory = innerFactory;
            }
        }
        return factory;
    }
}
