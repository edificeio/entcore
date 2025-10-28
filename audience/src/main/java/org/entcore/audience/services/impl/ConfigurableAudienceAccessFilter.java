package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.common.user.UserInfos;

import java.util.*;

/**
 * Configurable implementation of AudienceAccessFilter that selects the appropriate
 * implementation based on application configuration.
 * Each module can individually use the event-bus or NATS.
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
 *       "defaultType": "EVENTBUS", // by default: use the event-bus TODO: Removed
 *       "modules": {
 *         "communities": { "type": "NATS" }, // for module "communities" use NATS TODO: Removed
 *       },
 *       "natsModules": { //modules using NATS. For each one, provide the resourceTypes TODO: Replaces "defaultType" and "modules"
 *           "communities": [ "announcements" ],
 *           "foo": [ "bar", "baz" ]
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Behavior:
 * <ul>
 *   <li>If {@code config.broker.modules.<module>.type} is present it is used for that module.</li>
 *   <li>Otherwise {@code config.broker.defaultType} is used as the default.</li>
 *   <li>If no broker configuration is present the filter falls back to using the event bus.</li>
 * </ul>
 */
public class ConfigurableAudienceAccessFilter implements AudienceAccessFilter {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableAudienceAccessFilter.class);

    private final JsonObject config;
    private final NatsBrokerAudienceAccessFilter natsFilter;
    private final EventBusAudienceAccessFilter eventBusFilter;
    private final Map<String, List<String>> natsModulesConfig;

    public ConfigurableAudienceAccessFilter(final Vertx vertx, final JsonObject config) {
        this.config = config != null ? config.copy() : new JsonObject();
        // pre-create delegates to avoid creating them at each call
        this.eventBusFilter = new EventBusAudienceAccessFilter(vertx);

        // initialize NATS audience filters using provided config
        this.natsModulesConfig = new HashMap<>();
        JsonObject brokerConfig = this.config.getJsonObject("broker");
        if (brokerConfig != null) {
            JsonObject modulesConfig = brokerConfig.getJsonObject("natsModules");
            if (modulesConfig != null) {
                Set<String> modules = modulesConfig.fieldNames();
                for (String module : modules) {
                    final List<String> resourceTypes = modulesConfig.getJsonArray(module).getList();
                    natsModulesConfig.put(module, resourceTypes);
                }
            }
        }
        this.natsFilter = new NatsBrokerAudienceAccessFilter(vertx, natsModulesConfig);
    }

    @Override
    public Future<Boolean> canAccess(final String module, final String resourceType,
                                     final UserInfos user, final Set<String> resourceIds) {
        if (natsModulesConfig.containsKey(module)) {
            log.debug("ConfigurableAudienceAccessFilter: using NATS for module " + module);
            return natsFilter.canAccess(module, resourceType, user, resourceIds);
        } else {
            log.debug("ConfigurableAudienceAccessFilter: using EventBus for module " + module);
            return eventBusFilter.canAccess(module, resourceType, user, resourceIds);
        }
    }
}
