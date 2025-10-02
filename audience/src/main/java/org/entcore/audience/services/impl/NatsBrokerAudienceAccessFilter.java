package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.broker.api.BrokerFactory;
import org.entcore.broker.proxy.AudienceBrokerListener;
import org.entcore.broker.api.dto.audience.CheckResourceAccessRequestDTO;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.common.user.UserInfos;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of AudienceAccessFilter using NATS broker communication through BrokerFactory.
 * This implementation uses the AudienceBrokerListener interface to check resource access rights.
 */
public class NatsBrokerAudienceAccessFilter implements AudienceAccessFilter {
    private final Vertx vertx;
    
    public NatsBrokerAudienceAccessFilter(final Vertx vertx) {
        this.vertx = vertx;
    }
    
    @Override
    public Future<Boolean> canAccess(final String module, final String resourceType,
                                     final UserInfos user, final Set<String> resourceIds) {
        // Create the broker listener with address parameters for module and resourceType
        AudienceBrokerListener listener = BrokerFactory.create(
            AudienceBrokerListener.class, 
            vertx,
            new AddressParameter("module", module),
            new AddressParameter("resourceType", resourceType)
        );
        
        // Create request DTO
        CheckResourceAccessRequestDTO request = new CheckResourceAccessRequestDTO(
            module,
            resourceType,
            user.getUserId(),
            new HashSet<>(user.getGroupsIds()),
            resourceIds
        );
        
        // Make the request and map the response
        return listener.checkResourceAccess(request)
            .map(response -> {
                if (response.isSuccess()) {
                    return response.isAccess();
                } else {
                    throw new RuntimeException(response.getErrorMsg());
                }
            });
    }
}