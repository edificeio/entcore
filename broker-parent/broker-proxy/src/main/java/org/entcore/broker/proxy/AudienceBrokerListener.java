package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.audience.CheckResourceAccessRequestDTO;
import org.entcore.broker.api.dto.audience.CheckResourceAccessResponseDTO;

/**
 * This interface defines the methods that will be used to listen to events from the audience broker.
 * It provides functionality to check user access rights for specific resources.
 */
public interface AudienceBrokerListener {
    /**
     * This method is used to check if a user has access to specific resources.
     * @param request The request object containing the user information, module, resource type, and resource IDs.
     * @return A response object indicating if the user has access and containing potential error information.
     */
    @BrokerListener(subject = "audience.check.right.{module}.{resourceType}", proxy = true)
    Future<CheckResourceAccessResponseDTO> checkResourceAccess(CheckResourceAccessRequestDTO request);
}