package org.entcore.broker.config;

import io.nats.client.Options;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * <b>NATSBrokersConfig</b> provides a unified configuration mechanism for NATS brokers in both legacy (single broker) and modern (multi-broker) modes.<br>
 * <b>Features:</b>
 * <ul>
 *   <li>Supports both legacy and multi-broker configuration using a single <code>nats</code> key.</li>
 *   <li>Automatic detection of configuration mode based on the type of <code>nats</code> (JsonObject or JsonArray).</li>
 *   <li>Flexible routing: each broker can define routing patterns using NATS wildcards (<code>*</code>, <code>></code>).</li>
 * </ul>
 *
 * <h3>NATS Wildcards Syntax</h3>
 * <p>
 * NATS uses a subject-based routing system with two types of wildcards:
 * <ul>
 *   <li><b><code>*</code> (star)</b>: Matches exactly one subject segment.<br>
 *     Example: <code>foo.*</code> matches <code>foo.bar</code> but <b>not</b> <code>foo.bar.baz</code>.</li>
 *   <li><b><code>></code> (chevron)</b>: Matches zero or more subject segments (greedy).<br>
 *     Example: <code>foo.></code> matches <code>foo.bar</code>, <code>foo.bar.baz</code>, <code>foo</code>, etc.</li>
 * </ul>
 * <p>
 * This is the standard NATS subject matching syntax. For more details, see the <a href="https://docs.nats.io/nats-concepts/subjects#wildcards">NATS documentation</a>.
 *
 * <ul>
 *   <li>Default broker selection: at least one broker must be marked as <code>default</code>, otherwise the first is used.</li>
 *   <li>Full support for all NATS Java client properties (e.g., <code>io.nats.client.url</code>, <code>io.nats.client.username</code>, etc.).</li>
 *   <li>Backward compatibility: legacy configuration is still supported for seamless migration.</li>
 *   <li>Security: all sensitive connection parameters (username, password, TLS, etc.) are supported via the NATS client properties.</li>
 *   <li>Routing patterns can be as broad or as specific as needed, and support overlapping rules.</li>
 *   <li>Multi-broker mode enables advanced scenarios such as routing by subject</li>
 * </ul>
 *
 * <b>Example legacy configuration (single broker):</b>
 * <pre>
 * {
 *   "nats": {
 *     "io.nats.client.url": "tls://nats.example.com:4222",
 *     "io.nats.client.username": "demo",
 *     "io.nats.client.password": "demo",
 *     "io.nats.client.pinginterval": "PT15s"
 *   },
 *   "queue-name": "entcore"
 * }
 * </pre>
 *
 * <b>Example multi-broker configuration:</b>
 * <pre>
 * {
 *   "nats": [
 *     {
 *       "name": "broker1",
 *       "default": true,
 *       "routing": ["foo.*", "bar.>", "*"],
 *       "client": {
 *         "io.nats.client.url": "tls://broker1.example.com:4222",
 *         "io.nats.client.username": "user1",
 *         "io.nats.client.password": "pass1",
 *         "io.nats.client.pinginterval": "PT10s"
 *       }
 *     },
 *     {
 *       "name": "broker2",
 *       "routing": ["baz.>", "qux.*"],
 *       "client": {
 *         "io.nats.client.url": "tls://broker2.example.com:4222",
 *         "io.nats.client.username": "user2",
 *         "io.nats.client.password": "pass2",
 *         "io.nats.client.pinginterval": "PT20s"
 *       }
 *     }
 *   ],
 *   "queue-name": "entcore"
 * }
 * </pre>
 */
public class NATSBrokersConfig {

    private static final Logger log = LoggerFactory.getLogger(NATSBrokersConfig.class);

    private final List<NATSBrokerConfig> brokers;
    private final boolean isMultiBrokerMode;

    /**
     * Configuration for a single NATS broker
     */
    public static class NATSBrokerConfig {
        private final String name;
        private final boolean isDefault;
        private final List<String> routing;
        private final Options.Builder optionsBuilder;

        public NATSBrokerConfig(final String name, final boolean isDefault, final List<String> routing, final Options.Builder optionsBuilder) {
            this.name = name;
            this.isDefault = isDefault;
            this.routing = routing != null ? routing : Collections.emptyList();
            this.optionsBuilder = optionsBuilder;
        }

        public String getName() { return name; }
        public boolean isDefault() { return isDefault; }
        public List<String> getRouting() { return routing; }
        public io.nats.client.Options.Builder getOptionsBuilder() { return optionsBuilder; }
    }

    /**
     * Create configuration from Vertx config
     */
    public NATSBrokersConfig(final Vertx vertx) {
        final JsonObject config = vertx.getOrCreateContext().config();
        final Object natsValue = config.getValue("nats");
        if (natsValue instanceof JsonArray) {
            // Multi-broker configuration
            this.isMultiBrokerMode = true;
            this.brokers = parseMultiBrokerConfig((JsonArray) natsValue);
            log.info("Initialized multi-broker mode with " + brokers.size() + " brokers");
        } else if (natsValue instanceof JsonObject) {
            // Legacy single broker configuration
            this.isMultiBrokerMode = false;
            this.brokers = parseLegacyConfig((JsonObject) natsValue);
            log.info("Initialized legacy single broker mode");
        } else {
            throw new IllegalStateException("No NATS configuration found. Expected 'nats' as JsonObject (legacy) or JsonArray (multi-broker) in configuration");
        }
    }

    private List<NATSBrokerConfig> parseMultiBrokerConfig(final JsonArray natsBrokersArray) {
        final List<NATSBrokerConfig> configs = new ArrayList<>();

        for (int i = 0; i < natsBrokersArray.size(); i++) {
            final JsonObject brokerObj = natsBrokersArray.getJsonObject(i);
            final String name = brokerObj.getString("name");
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("Broker name is required for multi-broker configuration");
            }

            final boolean isDefault = brokerObj.getBoolean("default", false);
            final JsonArray routingArray = brokerObj.getJsonArray("routing");
            final List<String> routing = new ArrayList<>();
            if (routingArray != null) {
                for (int j = 0; j < routingArray.size(); j++) {
                    routing.add(routingArray.getString(j));
                }
            }

            final JsonObject natsConfig = brokerObj.getJsonObject("client");
            if (natsConfig == null) {
                throw new IllegalStateException("NATS configuration is required for broker: " + name);
            }

            final Options.Builder optionsBuilder = createOptionsBuilder(natsConfig);

            configs.add(new NATSBrokerConfig(name, isDefault, routing, optionsBuilder));
        }

        // Ensure at least one default broker
        final boolean hasDefault = configs.stream().anyMatch(NATSBrokerConfig::isDefault);
        if (!hasDefault && !configs.isEmpty()) {
            // Create new config with isDefault=true
            final NATSBrokerConfig firstConfig = configs.get(0);
            final NATSBrokerConfig defaultConfig = new NATSBrokerConfig(
                firstConfig.getName(),
                true,
                firstConfig.getRouting(),
                firstConfig.getOptionsBuilder()
            );
            configs.set(0, defaultConfig);
        }

        return configs;
    }

    private List<NATSBrokerConfig> parseLegacyConfig(final JsonObject natsConfig) {
        final Options.Builder optionsBuilder = createOptionsBuilder(natsConfig);
        final NATSBrokerConfig config = new NATSBrokerConfig("default", true, Arrays.asList("*"), optionsBuilder);
        return Arrays.asList(config);
    }

    private Options.Builder createOptionsBuilder(final JsonObject natsConfig) {
        final Properties properties = new Properties();
        natsConfig.getMap().forEach((key, value) -> {
            if (value instanceof String) {
                properties.put(key, value);
            } else if (value instanceof Number) {
                properties.put(key, ((Number) value).intValue());
            } else if (value instanceof Boolean) {
                properties.put(key, value);
            }
        });
        return new Options.Builder(properties);
    }

    public List<NATSBrokerConfig> getBrokers() {
        return brokers;
    }

    public boolean isMultiBrokerMode() {
        return isMultiBrokerMode;
    }

    public NATSBrokerConfig getDefaultBroker() {
        return brokers.stream()
            .filter(NATSBrokerConfig::isDefault)
            .findFirst()
            .orElse(brokers.get(0));
    }

    /**
     * Get the broker configuration for a given subject based on routing rules
     */
    public NATSBrokerConfig getBrokerForSubject(final String subject) {
        // Check specific routing first (non-default brokers)
        for (NATSBrokerConfig broker : brokers) {
            if (!broker.isDefault() && matchesRouting(subject, broker.getRouting())) {
                return broker;
            }
        }
        // Fall back to default broker
        return getDefaultBroker();
    }

    /**
     * Check if a subject matches any of the routing patterns
     */
    private boolean matchesRouting(String subject, List<String> routing) {
        for (String pattern : routing) {
            if (matchesPattern(subject, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match a subject against a NATS-style pattern
     * Supports wildcards: * (single segment) and > (multiple segments)
     */
    private boolean matchesPattern(String subject, String pattern) {
        if ("*".equals(pattern)) {
            return true; // Match everything
        }

        // Convert NATS pattern to regex
        final String regex = pattern
            .replace(".", "\\.")  // Escape dots
            .replace("*", "[^.]+") // * matches single segment
            .replace(">", ".*");   // > matches rest

        final Pattern compiledPattern = Pattern.compile("^" + regex + "$");
        return compiledPattern.matcher(subject).matches();
    }
}