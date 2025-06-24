package org.entcore.communication.listeners;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import org.entcore.broker.api.dto.communication.*;
import org.entcore.broker.proxy.CommunicationBrokerListener;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.services.CommunicationService.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CommunicationBrokerListener interface.
 * This class handles communication operations received through the message broker.
 */
public class CommunicationBrokerListenerImpl implements CommunicationBrokerListener {
    private static final Logger log = LoggerFactory.getLogger(CommunicationBrokerListenerImpl.class);
    private final CommunicationService communicationService;

    /**
     * Constructor for CommunicationBrokerListenerImpl.
     *
     * @param communicationService The communication service to handle operations
     */
    public CommunicationBrokerListenerImpl(final CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    /**
     * Creates a communication link between two groups.
     *
     * @param request The request containing source and target group IDs
     * @return Response with the result of the operation
     */
    @Override
    public Future<AddLinkBetweenGroupsResponseDTO> addLinkBetweenGroups(AddLinkBetweenGroupsRequestDTO request) {
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for addLinkBetweenGroups: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Extract parameters
        final String startGroupId = request.getStartGroupId();
        final String endGroupId = request.getEndGroupId();
        
        log.debug("Adding communication link between groups {} and {}", startGroupId, endGroupId);
        
        // Use Promise to bridge callback and Future APIs
        Promise<AddLinkBetweenGroupsResponseDTO> promise = Promise.promise();
        
        // Call service method to add the link
        communicationService.addLink(startGroupId, endGroupId, event -> {
            if (event.isRight()) {
                promise.complete(new AddLinkBetweenGroupsResponseDTO(true));
            } else {
                log.error("Failed to add link between groups {} and {}: {}", 
                        startGroupId, endGroupId, event.left().getValue());
                promise.complete(new AddLinkBetweenGroupsResponseDTO(false));
            }
        });
        
        return promise.future();
    }

    /**
     * Adds communication links between a group and its users.
     *
     * @param request The request containing group ID and direction
     * @return Response with the result of the operation
     */
    @Override
    public Future<AddCommunicationLinksResponseDTO> addCommunicationLinks(AddCommunicationLinksRequestDTO request) {
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for addCommunicationLinks: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Extract parameters
        final String groupId = request.getGroupId();
        final Direction direction = Direction.fromString(request.getDirection());
        
        log.debug("Adding communication links for group {} with direction {}", groupId, direction);
        
        // Use Promise to bridge callback and Future APIs
        Promise<AddCommunicationLinksResponseDTO> promise = Promise.promise();
        
        // Call service method to add communication links
        communicationService.addLinkWithUsers(groupId, direction, event -> {
            if (event.isRight()) {
                promise.complete(new AddCommunicationLinksResponseDTO(true));
            } else {
                log.error("Failed to add communication links for group {}: {}", 
                        groupId, event.left().getValue());
                promise.complete(new AddCommunicationLinksResponseDTO(false));
            }
        });
        
        return promise.future();
    }

    /**
     * Removes communication links between a group and its users.
     *
     * @param request The request containing group ID and direction
     * @return Response with the result of the operation
     */
    @Override
    public Future<RemoveCommunicationLinksResponseDTO> removeCommunicationLinks(RemoveCommunicationLinksRequestDTO request) {
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for removeCommunicationLinks: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Extract parameters
        final String groupId = request.getGroupId();
        final Direction direction = Direction.fromString(request.getDirection());
        
        log.debug("Removing communication links for group {} with direction {}", groupId, direction);
        
        // Use Promise to bridge callback and Future APIs
        Promise<RemoveCommunicationLinksResponseDTO> promise = Promise.promise();
        
        // Call service method to remove communication links
        communicationService.removeLinkWithUsers(groupId, direction, event -> {
            if (event.isRight()) {
                promise.complete(new RemoveCommunicationLinksResponseDTO(true));
            } else {
                log.error("Failed to remove communication links for group {}: {}", 
                        groupId, event.left().getValue());
                promise.complete(new RemoveCommunicationLinksResponseDTO(false));
            }
        });
        
        return promise.future();
    }

    /**
     * Recreates communication links between a group and its users.
     * This involves removing existing links and then adding them back.
     *
     * @param request The request containing group ID and direction
     * @return Response with the result of the operation
     */
    @Override
    public Future<RecreateCommunicationLinksResponseDTO> recreateCommunicationLinks(RecreateCommunicationLinksRequestDTO request) {
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for recreateCommunicationLinks: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Extract parameters
        final String groupId = request.getGroupId();
        final Direction direction = Direction.fromString(request.getDirection());
        
        log.debug("Recreating communication links for group {} with direction {}", groupId, direction);
        
        // Use Promise for the first operation
        Promise<JsonObject> removePromise = Promise.promise();
        
        // First remove existing communication links
        communicationService.removeLinkWithUsers(groupId, direction, removeResult -> {
            if (removeResult.isRight()) {
                removePromise.complete(removeResult.right().getValue());
            } else {
                log.error("Failed to remove communication links for group {}: {}", 
                        groupId, removeResult.left().getValue());
                removePromise.fail(removeResult.left().getValue());
            }
        });
        
        // Chain add operation after remove is complete
        return removePromise.future().compose(removeResult -> {
            Promise<RecreateCommunicationLinksResponseDTO> addPromise = Promise.promise();
            
            // Call service method to add communication links back
            communicationService.addLinkWithUsers(groupId, direction, addResult -> {
                if (addResult.isRight()) {
                    addPromise.complete(new RecreateCommunicationLinksResponseDTO(true));
                } else {
                    log.error("Failed to add communication links during recreation for group {}: {}", 
                            groupId, addResult.left().getValue());
                    addPromise.complete(new RecreateCommunicationLinksResponseDTO(false));
                }
            });
            
            return addPromise.future();
        }).otherwise(error -> {
            log.error("Failed during recreation of communication links for group {}: {}", 
                    groupId, error.getMessage());
            return new RecreateCommunicationLinksResponseDTO(false);
        });
    }
}