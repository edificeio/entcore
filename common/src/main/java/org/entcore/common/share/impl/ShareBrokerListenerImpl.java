package org.entcore.common.share.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.shares.RemoveGroupSharesRequestDTO;
import org.entcore.broker.api.dto.shares.RemoveGroupSharesResponseDTO;
import org.entcore.broker.api.dto.shares.SharesResponseDTO;
import org.entcore.broker.api.dto.shares.UpsertGroupSharesRequestDTO;
import org.entcore.broker.api.dto.shares.UpsertGroupSharesResponseDTO;
import org.entcore.broker.proxy.ShareBrokerListener;
import org.entcore.common.share.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ShareBrokerListener that manages group shares in MongoDB.
 * This listener handles broker events for manipulating shares (permissions) for groups.
 */
public class ShareBrokerListenerImpl implements ShareBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(ShareBrokerListenerImpl.class);
    private final ShareService shareService;

    /**
     * Creates a new ShareBrokerListenerMongo with specified resource ID field.
     * 
     * @param shareService The ShareService instance to be used for share operations
     */
    public ShareBrokerListenerImpl(final ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Upserts (creates or updates) group shares.
     * This method will add or update the specified permissions for a group on a resource.
     *
     * @param request The request containing resource ID, group ID and permissions
     * @return Future with the response containing the updated shares
     */
    @Override
    public Future<UpsertGroupSharesResponseDTO> upsertGroupShares(UpsertGroupSharesRequestDTO request) {
        Promise<UpsertGroupSharesResponseDTO> promise = Promise.promise();
        // Validate the request
        if (request == null || !request.isValid()) {
            log.error("Invalid request for upsertGroupShares: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }

        // Convert permissions to JSON array format expected by MongoDbShareService
        final JsonObject share = new JsonObject().put("groupId", request.getGroupId());
        for (String permission : request.getPermissions()) {
            share.put(permission, true);
        }
        
        // Call the share service to update the shares
        shareService.shareInfosWithoutVisible(request.getCurrentUserId(), request.getResourceId(), shareEvent -> {
            if (shareEvent.isRight()) {
                final JsonArray sharedArray = shareEvent.right().getValue();
                final Optional<JsonObject> found = sharedArray.stream().filter(shareObj -> shareObj instanceof JsonObject)
                        .map(shareObj -> (JsonObject) shareObj)
                        .filter(shareObj -> shareObj.getString("groupId").equals(request.getGroupId())).findFirst();
                if (found.isPresent()) {
                    found.get().mergeIn(share);
                } else {
                    sharedArray.add(share);
                }
                final JsonObject sharedObject = shareService.sharedArrayToSharedObject(sharedArray);
                shareService.share(request.getCurrentUserId(), request.getResourceId(), sharedObject, event -> {
                    if (event.isLeft()) {
                        log.error("Error sharing resource: {}", event.left().getValue());
                        promise.fail(event.left().getValue());
                    } else {
                        // Convert sharedArray to response DTO
                        final List<SharesResponseDTO> sharedDtoList = convertSharedArrayToDto(sharedArray);
                        promise.complete(new UpsertGroupSharesResponseDTO(sharedDtoList));
                    }
                });
            } else {
                log.error("Error updating shares: {}", shareEvent.left().getValue());
                promise.fail(shareEvent.left().getValue());
            }
        });
        
        return promise.future();
    }

    /**
     * Removes group shares from a resource.
     * This method will remove the specified permissions for a group on a resource.
     *
     * @param request The request containing resource ID, group ID and permissions to remove
     * @return Future with the response indicating success or failure
     */
    @Override
    public Future<RemoveGroupSharesResponseDTO> removeGroupShares(RemoveGroupSharesRequestDTO request) {
        Promise<RemoveGroupSharesResponseDTO> promise = Promise.promise();

        if (request == null || request.getResourceId() == null || request.getGroupId() == null) {
            log.error("Invalid request for removeGroupShares: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }

        // Call the share service to update the shares
        shareService.shareInfosWithoutVisible(request.getCurrentUserId(), request.getResourceId(), shareEvent -> {
            if (shareEvent.isRight()) {
                final JsonArray sharedArray = shareEvent.right().getValue();
                final List<JsonObject> sharedList = sharedArray.stream()
                        .filter(shareObj -> shareObj instanceof JsonObject)
                        .map(shareObj -> (JsonObject) shareObj)
                        .collect(Collectors.toList());
                final boolean removed = sharedList.removeIf(object -> object.getString("groupId", "").equals(request.getGroupId()));
                if(!removed){
                    final List<SharesResponseDTO> sharedDtoList = convertSharedArrayToDto(sharedArray);
                    promise.complete(new RemoveGroupSharesResponseDTO(sharedDtoList));
                    return;
                }
                final JsonObject sharedObject = shareService.sharedArrayToSharedObject(sharedArray);
                shareService.share(request.getCurrentUserId(), request.getResourceId(), sharedObject, event -> {
                    if (event.isLeft()) {
                        log.error("Error sharing resource: {}", event.left().getValue());
                        promise.fail(event.left().getValue());
                    } else {
                        // Convert sharedArray to response DTO
                        final List<SharesResponseDTO> sharedDtoList = convertSharedArrayToDto(sharedArray);
                        promise.complete(new RemoveGroupSharesResponseDTO(sharedDtoList));
                    }
                });
            } else {
                log.error("Error updating shares: {}", shareEvent.left().getValue());
                promise.fail(shareEvent.left().getValue());
            }
        });
        
        return promise.future();
    }

    /**
     * Converts a JsonArray of shared objects to a list of SharesResponseDTO objects.
     * This method extracts user and group permissions from shared objects and formats them for response.
     *
     * @param sharedArray The JsonArray containing shared objects
     * @return A list of SharesResponseDTO objects
     */
    private List<SharesResponseDTO> convertSharedArrayToDto(JsonArray sharedArray) {
        final List<SharesResponseDTO> sharedDtoList = new ArrayList<>();
        
        for (int i = 0; i < sharedArray.size(); i++) {
            final JsonObject shareObj = sharedArray.getJsonObject(i);
            if (shareObj != null) {
                final String userId = shareObj.getString("userId");
                final String groupId = shareObj.getString("groupId");
                final List<String> permissions = new ArrayList<>(shareObj.fieldNames());
                
                if (userId != null) {
                    permissions.remove("userId");
                    final SharesResponseDTO sharesResponse = new SharesResponseDTO(
                            userId, 
                            SharesResponseDTO.Kind.User, 
                            permissions
                    );
                    sharedDtoList.add(sharesResponse);
                } else if (groupId != null) {
                    permissions.remove("groupId");
                    final SharesResponseDTO sharesResponse = new SharesResponseDTO(
                            groupId, 
                            SharesResponseDTO.Kind.Group, 
                            permissions
                    );
                    sharedDtoList.add(sharesResponse);
                }
            }
        }
        
        return sharedDtoList;
    }
}