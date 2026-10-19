package org.entcore.workspace.listeners;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.shares.*;
import org.entcore.broker.proxy.ShareBrokerListener;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.share.ShareRoles;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.controllers.WorkspaceController;

/**
 * Listener for handling share operations on workspace documents.
 * Uses SHARE_OBJECT to send complete share object with all groups.
 * Reuses FolderManager.share() which handles inherited shares via InheritShareComputer.
 * 
 * Broker subjects:
 * - upsertGroupShares: Adds/updates group shares on a document
 * - removeGroupShares: Removes group shares from a document
 */
public class WorkspaceShareBrokerListener implements ShareBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceShareBrokerListener.class);

    private final Vertx vertx;
    private final ShareService shareService;
    private final FolderManager folderManager;
    private final Map<String, SecuredAction> securedActions;

    /**
     * Creates a new WorkspaceShareBrokerListener.
     *
     * @param vertx The Vert.x instance
     * @param securedActions Map of secured actions for role-to-action conversion
     * @param shareService Service for share operations
     * @param folderManager Folder manager for document operations and inherited shares
     */
    public WorkspaceShareBrokerListener(
            final Vertx vertx,
            final Map<String, SecuredAction> securedActions,
            final ShareService shareService,
            final FolderManager folderManager) {
        this.vertx = vertx;
        this.securedActions = securedActions;
        this.shareService = shareService;
        this.folderManager = folderManager;
    }

    /**
     * Upserts (creates or updates) group shares using SHARE_OBJECT.
     * Gets existing shares, adds/updates the group, and applies the complete share object.
     */
    @Override
    public Future<UpsertGroupSharesResponseDTO> upsertGroupShares(UpsertGroupSharesRequestDTO request) {
        Promise<UpsertGroupSharesResponseDTO> promise = Promise.promise();

        if (request == null || !request.isValid()) {
            log.error("Invalid request for upsertGroupShares: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }

        final String resourceId = request.getResourceId();
        final String groupId = request.getGroupId();
        final List<String> permissions = request.getPermissions();
        final String currentUserId = request.getCurrentUserId();

        // Convert permissions to actions array
        final JsonArray actions = new JsonArray();
        for (final String permission : permissions) {
            final List<String> actionList = ShareRoles.getSecuredActionNameByRole(permission, this.securedActions);
            for (String action : actionList) {
                actions.add(action);
            }
        }

        // Step 1: Get existing shares
        shareService.shareInfosWithoutVisible(currentUserId, resourceId, infoEvent -> {
            if (infoEvent.isLeft()) {
                log.error("Error getting share info: " + infoEvent.left().getValue());
                promise.fail(infoEvent.left().getValue());
                return;
            }

            // Step 2: Convert to JsonObject format
            final JsonArray sharedArray = infoEvent.right().getValue();
            final JsonObject sharedObject = shareService.sharedArrayToSharedObject(sharedArray);

            // Step 3: Get or create groups object
            final JsonObject groups = sharedObject.getJsonObject("groups", new JsonObject());

            // Step 4: Add/update the group with its permissions
            groups.put(groupId, actions);
            sharedObject.put("groups", groups);

            // Step 5: Create minimal UserInfos with just userId
            final UserInfos user = new UserInfos();
            user.setUserId(currentUserId);

            // Step 6: Use SHARE_OBJECT to apply the complete share object (as system operation to bypass permission check)
            final ElementShareOperations shareOps = ElementShareOperations.addShareObjectAsSystem(
                    WorkspaceController.SHARED_ACTION,
                    user,
                    sharedObject
            );

            // Step 7: FolderManager.share() handles:
            // - Permission check
            // - Share application via ShareService.share()
            // - Inherited shares via InheritShareComputer
            // - MongoDB update
            folderManager.share(resourceId, shareOps, shareEvent -> {
                if (shareEvent.failed()) {
                    log.error("Error sharing document " + resourceId, shareEvent.cause());
                    promise.fail(shareEvent.cause());
                    return;
                }

                // Step 8: Return the updated shares
                final JsonArray updatedShares = shareEvent.result().getJsonArray("shared", new JsonArray());
                final List<SharesResponseDTO> dtoList = convertSharedArrayToDto(updatedShares);
                promise.complete(new UpsertGroupSharesResponseDTO(dtoList));
            });
        });

        return promise.future();
    }

    /**
     * Checks whether a user holds a share role on a document or folder, directly or through one of his groups.
     * Same check as WorkspaceResourcesProvider: the owner is granted, otherwise inherited shares
     * (including parent folders shares) must contain an action of the role.
     */
    @Override
    public Future<CheckResourceRightResponseDTO> checkResourceRight(CheckResourceRightRequestDTO request) {
        if (request == null || !request.isValid()) {
            log.error("Invalid request for checkResourceRight: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        // All actions of a role are granted together: checking one of them is enough
        final Optional<String> roleAction = ShareRoles.getSecuredActionNameByRole(request.getRole(), securedActions)
                .stream().sorted().findFirst();
        if (!roleAction.isPresent()) {
            log.error("No secured action found for role in checkResourceRight: {}", request);
            return Future.failedFuture("role.invalid");
        }
        final Promise<CheckResourceRightResponseDTO> promise = Promise.promise();
        // Load user's groups (same source as HTTP resource filters)
        UserUtils.getUserInfos(vertx.eventBus(), request.getUserId(), user -> {
            if (user == null) {
                promise.complete(new CheckResourceRightResponseDTO(false));
                return;
            }
            // Matches the element if the user is owner or has the action in its inherited shares
            final ElementQuery query = new ElementQuery(true);
            query.setId(request.getResourceId());
            query.setActionExistsInInheritedShares(roleAction.get());
            folderManager.countByQuery(query, user, countEvent -> {
                if (countEvent.failed()) {
                    log.error("Error checking right on document " + request.getResourceId(), countEvent.cause());
                    promise.fail(countEvent.cause());
                    return;
                }
                promise.complete(new CheckResourceRightResponseDTO(countEvent.result() == 1));
            });
        });
        return promise.future();
    }

    /**
     * Removes group shares using SHARE_OBJECT.
     * Gets existing shares, removes the group, and applies the complete share object.
     */
    @Override
    public Future<RemoveGroupSharesResponseDTO> removeGroupShares(RemoveGroupSharesRequestDTO request) {
        Promise<RemoveGroupSharesResponseDTO> promise = Promise.promise();

        if (request == null || request.getResourceId() == null || request.getGroupId() == null) {
            log.error("Invalid request for removeGroupShares: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }

        final String resourceId = request.getResourceId();
        final String groupId = request.getGroupId();
        final String currentUserId = request.getCurrentUserId();

        // Step 1: Get existing shares
        shareService.shareInfosWithoutVisible(currentUserId, resourceId, infoEvent -> {
            if (infoEvent.isLeft()) {
                promise.fail(infoEvent.left().getValue());
                return;
            }

            // Step 2: Convert to JsonObject format
            final JsonArray sharedArray = infoEvent.right().getValue();
            final JsonObject sharedObject = shareService.sharedArrayToSharedObject(sharedArray);

            // Step 3: Remove the group from groups object
            final JsonObject groups = sharedObject.getJsonObject("groups", new JsonObject());
            groups.remove(groupId);
            sharedObject.put("groups", groups);

            // Step 4: Create minimal UserInfos with just userId
            final UserInfos user = new UserInfos();
            user.setUserId(currentUserId);

            // Step 5: Use SHARE_OBJECT with the updated share object (group removed) as system operation
            final ElementShareOperations shareOps = ElementShareOperations.addShareObjectAsSystem(
                    WorkspaceController.SHARED_ACTION,
                    user,
                    sharedObject
            );

            folderManager.share(resourceId, shareOps, shareEvent -> {
                if (shareEvent.failed()) {
                    promise.fail(shareEvent.cause());
                    return;
                }

                final JsonArray updatedShares = shareEvent.result().getJsonArray("shared", new JsonArray());
                final List<SharesResponseDTO> dtoList = convertSharedArrayToDto(updatedShares);
                promise.complete(new RemoveGroupSharesResponseDTO(dtoList));
            });
        });

        return promise.future();
    }

    /**
     * Converts a JsonArray of shared objects to a list of SharesResponseDTO objects.
     */
    private static List<SharesResponseDTO> convertSharedArrayToDto(final JsonArray sharedArray) {
        final List<SharesResponseDTO> sharedDtoList = new ArrayList<>();
        
        for (int i = 0; i < sharedArray.size(); i++) {
            final JsonObject shareObj = sharedArray.getJsonObject(i);
            if (shareObj != null) {
                final String userId = shareObj.getString("userId");
                final String gId = shareObj.getString("groupId");
                final List<String> permissions = new ArrayList<>(shareObj.fieldNames());
                
                if (userId != null) {
                    permissions.remove("userId");
                    sharedDtoList.add(new SharesResponseDTO(
                            userId, 
                            SharesResponseDTO.Kind.User, 
                            permissions
                    ));
                } else if (gId != null) {
                    permissions.remove("groupId");
                    sharedDtoList.add(new SharesResponseDTO(
                            gId, 
                            SharesResponseDTO.Kind.Group, 
                            permissions
                    ));
                }
            }
        }
        
        return sharedDtoList;
    }
}
