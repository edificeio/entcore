package org.entcore.broker.client;

import io.nats.vertx.NatsClient;
import io.nats.vertx.NatsOptions;
import io.vertx.core.Vertx;
import org.entcore.broker.config.NATSBrokersConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Legacy NATS broker client for single connection.
 * 
 * @deprecated Use {@link NATSMultiBrokerClient} for new implementations.
 *             This class is maintained for backward compatibility only.
 *             
 * Migration guide:
 * - Replace NATSBrokerClient with NATSMultiBrokerClient configured with a single broker
 *   for equivalent functionality with better architecture and multi-broker support.
 */
@Deprecated
public class NATSBrokerClient extends AbstractNATSBrokerClient {

    private final NatsClient natsClient;
    private final String queueName;

    /**
     * Creates a NATS broker client from Vertx configuration.
     * This is the legacy constructor for backward compatibility.
     * Uses NATSBrokersConfig in legacy mode (single broker).
     *
     * @param vertx The Vert.x instance
     * @param config The NATSBrokersConfig
     */
    public NATSBrokerClient(final Vertx vertx, final NATSBrokersConfig config) {
        super(vertx);
        this.queueName = vertx.getOrCreateContext().config().getString("queue-name", "entcore");

        // will use legacy "nats" config
        final NATSBrokersConfig.NATSBrokerConfig brokerConfig = config.getDefaultBroker();
        
        final NatsOptions natsOptions = new NatsOptions()
            .setNatsBuilder(brokerConfig.getOptionsBuilder())
            .setVertx(vertx);
        this.natsClient = NatsClient.create(natsOptions);
    }
    
    // ============================================================
    // ABSTRACT METHODS IMPLEMENTATION
    // ============================================================
    
    @Override
    protected NatsClient getNatsClientForSubject(final String subject) {
        // Single broker always returns the same client
        return natsClient;
    }
    
    @Override
    protected List<NatsClient> getAllNatsClients() {
        // Single broker has only one client
        return Arrays.asList(natsClient);
    }
    
    @Override
    protected String getQueueName() {
        return queueName;
    }
}
