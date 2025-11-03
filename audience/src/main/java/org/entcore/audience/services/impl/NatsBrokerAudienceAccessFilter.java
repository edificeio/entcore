package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.broker.api.BrokerProxyFactory;
import org.entcore.broker.proxy.AudienceListener;
import org.entcore.broker.api.dto.audience.CheckResourceAccessRequestDTO;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.common.user.UserInfos;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of AudienceAccessFilter using NATS broker communication through BrokerFactory.
 * This implementation uses the AudienceBrokerListener interface to check resource access rights.
 */
public class NatsBrokerAudienceAccessFilter implements AudienceAccessFilter {
    private static final Logger log = LoggerFactory.getLogger(NatsBrokerAudienceAccessFilter.class);
    private final Map<String, AudienceListener> clients;

    public NatsBrokerAudienceAccessFilter(final Vertx vertx, final Map<String, List<String>> modulesConfig) {
        this.clients = new HashMap<>();
        for (Map.Entry<String, List<String>> moduleConfig : modulesConfig.entrySet()) {
            final String moduleName = moduleConfig.getKey();
            final List<String> types = moduleConfig.getValue();
            for (String resourceType : types) {
                // Create the broker listener with address parameters for module and resourceType
                AudienceListener listener = BrokerProxyFactory.create(
                    AudienceListener.class,
                    vertx,
                    new AddressParameter("module", moduleName),
                    new AddressParameter("resourceType", resourceType)
                );
                clients.put(String.format("%s.%s", moduleName, resourceType), listener);
            }
        }
    }

    @Override
    public Future<Boolean> canAccess(final String module, final String resourceType,
                                     final UserInfos user, final Set<String> resourceIds) {
        // Create request DTO
        CheckResourceAccessRequestDTO request = new CheckResourceAccessRequestDTO(
            module,
            resourceType,
            user.getUserId(),
            new HashSet<>(user.getGroupsIds()),
            resourceIds
        );

        // Make the request and map the response
        final String clientId = String.format("%s.%s", module, resourceType);
        if (!this.clients.containsKey(clientId)) {
            return Future.failedFuture(String.format("NATS listener not configured for %s resources", clientId));
        }
        return this.clients.get(clientId)
            .checkResourceAccess(request)
            .map(response -> {
                if (response.isSuccess()) {
                    return response.isAccess();
                } else {
                    log.error(String.format("Error while checking access for %s resources", clientId));
                    throw new RuntimeException(response.getErrorMsg());
                }
            });
    }
}
