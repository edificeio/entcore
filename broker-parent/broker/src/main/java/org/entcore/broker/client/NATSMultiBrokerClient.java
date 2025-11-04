package org.entcore.broker.client;

import io.nats.vertx.NatsClient;
import io.nats.vertx.NatsOptions;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.config.NATSBrokersConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * <b>NATSMultiBrokerClient</b> manages multiple NATS connections and provides advanced subject-based routing.
 * <br>
 * <b>Features:</b>
 * <ul>
 *   <li>Supports multiple NATS clusters, each with its own configuration and credentials.</li>
 *   <li>Subject-based routing: each broker can define routing patterns using NATS wildcards (<code>*</code>, <code>></code>).</li>
 *   <li>Automatic selection of the correct NATS client for each subject, based on the routing list.</li>
 *   <li>Fallback to a default broker if no routing rule matches.</li>
 *   <li>Full compatibility with the <code>NATSBrokersConfig</code> configuration system (see that class for config examples).</li>
 * </ul>
 *
 * <b>How routing works:</b>
 * <ul>
 *   <li>When a message is published or a subscription is created, the subject is matched against each broker's routing patterns.</li>
 *   <li>The first matching non-default broker is selected; otherwise, the default broker is used.</li>
 *   <li>Routing patterns support NATS wildcards (<code>*</code> for a single segment, <code>></code> for multiple segments).</li>
 *   <li>Overlapping and catch-all rules are supported (e.g., <code>*</code> or <code>></code>).</li>
 * </ul>
 *
 * <b>Usage notes:</b>
 * <ul>
 *   <li>Legacy single-broker deployments should use <code>NATSBrokerClient</code> for backward compatibility.</li>
 *   <li>See <code>NATSBrokersConfig</code> for configuration details and examples.</li>
 * </ul>
 */
public class NATSMultiBrokerClient extends AbstractNATSBrokerClient {

    private static final Logger log = LoggerFactory.getLogger(NATSMultiBrokerClient.class);
    
    private final Map<String, NatsClient> clients;
    private final NATSBrokersConfig config;
    private final String queueName;

    /**
     * Creates a multi-broker NATS client from configuration.
     * 
     * @param vertx The Vert.x instance
     * @param config The brokers configuration
     */
    public NATSMultiBrokerClient(Vertx vertx, NATSBrokersConfig config) {
        super(vertx);
        this.config = config;
        this.clients = new HashMap<>();
        this.queueName = vertx.getOrCreateContext().config().getString("queue-name", "entcore");

        // Create a NatsClient for each broker configuration
        for (NATSBrokersConfig.NATSBrokerConfig brokerConfig : config.getBrokers()) {
            NatsClient client = createNatsClient(vertx, brokerConfig);
            clients.put(brokerConfig.getName(), client);
            log.info("Created NATS client for broker: " + brokerConfig.getName());
        }
    }
    
    /**
     * Creates a NatsClient from broker configuration.
     * 
     * @param vertx The Vert.x instance
     * @param brokerConfig The broker configuration
     * @return The created NatsClient
     */
    private NatsClient createNatsClient(Vertx vertx, NATSBrokersConfig.NATSBrokerConfig brokerConfig) {
        final NatsOptions natsOptions = new NatsOptions()
            .setNatsBuilder(brokerConfig.getOptionsBuilder())
            .setVertx(vertx);
        return NatsClient.create(natsOptions);
    }
    
    // ============================================================
    // ABSTRACT METHODS IMPLEMENTATION
    // ============================================================
    
    @Override
    protected NatsClient getNatsClientForSubject(final String subject) {
        // Use routing configuration to find the appropriate broker
        NATSBrokersConfig.NATSBrokerConfig brokerConfig = config.getBrokerForSubject(subject);
        NatsClient client = clients.get(brokerConfig.getName());
        
        if (client == null) {
            log.warn("No broker client found for subject: " + subject + ", using default");
            // Try to get default broker
            brokerConfig = config.getDefaultBroker();
            if (brokerConfig != null) {
                client = clients.get(brokerConfig.getName());
            }
            
            if (client == null && !clients.isEmpty()) {
                // Fallback to first available client
                client = clients.values().iterator().next();
                log.warn("Using fallback client for subject: " + subject);
            }
        }
        
        return client;
    }
    
    @Override
    protected List<NatsClient> getAllNatsClients() {
        // Multi-broker returns all managed clients
        return new ArrayList<>(clients.values());
    }
    
    @Override
    protected String getQueueName() {
        return queueName;
    }
}
