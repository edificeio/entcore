package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.common.user.UserInfos;

import java.util.Set;

/**
 * Configurable implementation of AudienceAccessFilter that selects the appropriate
 * implementation at call-time based on application configuration.
 * Each module can individually use NATS or the local event bus.
 *
 * <p>Configuration
 * <p>The service reads its configuration under the key {@code config}. The broker
 * settings must be placed under {@code config.broker}. Module-specific overrides
 * are supported. Example configuration (JSON):
 *
 * <pre>
 * {
 *   "config": {
 *     "broker": {
 *       "enabled": true,               // default: use NATS when true, event-bus when false
 *       "modules": {
 *         "audience": { "enabled": false }, // for module "audience" use event-bus
 *         "otherModule": { "enabled": true } // for "otherModule" use NATS
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Behavior:
 * <ul>
 *   <li>If {@code config.broker.modules.<module>.enabled} is present it is used for that module.</li>
 *   <li>Otherwise {@code config.broker.enabled} is used as the default.</li>
 *   <li>If no broker configuration is present the filter falls back to using the local
 *       event bus.</li>
 * </ul>
 *
 * <p>Usage: the decision to use NATS or the event-bus is taken at each call to
 * {@link #canAccess(String, String, org.entcore.common.user.UserInfos, java.util.Set)}</p>
 */
public class ConfigurableAudienceAccessFilter implements AudienceAccessFilter {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableAudienceAccessFilter.class);

    private final Vertx vertx;
    private final JsonObject config;
    private final NatsBrokerAudienceAccessFilter natsFilter;
    private final EventBusAudienceAccessFilter eventBusFilter;

    public ConfigurableAudienceAccessFilter(final Vertx vertx, final JsonObject config) {
        this.vertx = vertx;
        this.config = config != null ? config.copy() : new JsonObject();
        // pre-create delegates to avoid creating them at each call
        this.natsFilter = new NatsBrokerAudienceAccessFilter(vertx);
        this.eventBusFilter = new EventBusAudienceAccessFilter(vertx);
    }

    /**
     * Determines if NATS should be used for the given module based on configuration.
     * Module-specific config overrides global broker.enabled flag.
     *
     * Expected config shape:
     * {
     *   "broker": {
     *     "enabled": true,
     *     "modules": {
     *       "audience": { "enabled": false },
     *       "otherModule": { "enabled": true }
     *     }
     *   }
     * }
     *
     * @param module the module name to check
     * @return true if NATS should be used for this module, false otherwise
     */
    private boolean isNatsEnabledForModule(final String module) {
        JsonObject brokerConfig = config.getJsonObject("broker");
        if (brokerConfig == null) {
            return false;
        }
        JsonObject modules = brokerConfig.getJsonObject("modules");
        if (modules != null) {
            JsonObject moduleConfig = modules.getJsonObject(module);
            if (moduleConfig != null) {
                return moduleConfig.getBoolean("enabled", brokerConfig.getBoolean("enabled", false));
            }
        }
        return brokerConfig.getBoolean("enabled", false);
    }

    @Override
    public Future<Boolean> canAccess(final String module, final String resourceType,
                                     final UserInfos user, final Set<String> resourceIds) {
        boolean useNats = isNatsEnabledForModule(module);
        if (useNats) {
            log.debug("ConfigurableAudienceAccessFilter: using NATS for module " + module);
            return natsFilter.canAccess(module, resourceType, user, resourceIds);
        } else {
            log.debug("ConfigurableAudienceAccessFilter: using EventBus for module " + module);
            return eventBusFilter.canAccess(module, resourceType, user, resourceIds);
        }
    }
}