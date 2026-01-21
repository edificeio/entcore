package org.entcore.workspace.listeners;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.shares.*;
import org.entcore.broker.proxy.ShareBrokerListener;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.share.ShareRoles;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;
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

    private final ShareService shareService;
    private final FolderManager folderManager;
    private final Map<String, SecuredAction> securedActions;

    /**
     * Creates a new WorkspaceShareBrokerListener.
     * 
     * @param securedActions Map of secured actions for role-to-action conversion
     * @param shareService Service for share operations
     * @param folderManager Folder manager for document operations and inherited shares
     */
    public WorkspaceShareBrokerListener(
            final Map<String, SecuredAction> securedActions,
            final ShareService shareService,
            final FolderManager folderManager) {
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
