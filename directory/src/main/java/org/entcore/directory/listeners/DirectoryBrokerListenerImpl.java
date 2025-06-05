package org.entcore.directory.listeners;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.entcore.broker.api.dto.directory.*;
import org.entcore.broker.proxy.DirectoryBrokerListener;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultGroupService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the DirectoryBrokerListener interface.
 * This class handles directory operations received through the message broker.
 */
public class DirectoryBrokerListenerImpl implements DirectoryBrokerListener {
    
    private static final Logger log = LoggerFactory.getLogger(DirectoryBrokerListenerImpl.class);
    private final GroupService groupService;
    private final UserService userService;

    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param vertx The Vertx instance
     */
    public DirectoryBrokerListenerImpl(Vertx vertx, UserService userService) {
        this(new DefaultGroupService(vertx.eventBus()), userService);
    }
    
    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param groupService The group service to handle group operations
     * @param userService The user service to handle user operations
     */
    public DirectoryBrokerListenerImpl(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    /**
     * Creates a manual group in the directory.
     *
     * @param request The request containing group details
     * @return Response with the created group information
     */
    @Override
    public Future<CreateGroupResponseDTO> createManualGroup(CreateGroupRequestDTO request) {
        final Promise<CreateGroupResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for createManualGroup: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }

        // Create group data
        final JsonObject group = new JsonObject()
                .put("name", request.getName())
                // an empty id means a new group
                .put("id", "");
        // an empty externalId means no externalId
        if (request.getExternalId() != null) {
            group.put("externalId", request.getExternalId());
        }
        // an empty structureId means no structure
        final String structureId = request.getStructureId() != null ? request.getStructureId(): "";
        // an empty classId means no class
        final String classId = request.getClassId() != null ? request.getStructureId(): "";

        // Create group using existing service
        groupService.createOrUpdateManual(group, structureId, classId, event -> {
            if (event.isRight()) {
                final String id = event.right().getValue().getString("id");
                promise.complete(new CreateGroupResponseDTO(id));
            } else {
                log.error("Error creating group: {}", event.left().getValue());
                promise.fail(event.left().getValue());
            }
        });

        return promise.future();
    }

    /**
     * Updates an existing manual group in the directory.
     *
     * @param request The request containing updated group details
     * @return Response with the update result
     */
    @Override
    public Future<UpdateGroupResponseDTO> updateManualGroup(UpdateGroupRequestDTO request) {
        final Promise<UpdateGroupResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for updateManualGroup: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        // If groupExternalId is provided, fetch the group ID from the database
        final Future<String> groupIdFuture = request.getId() != null ? Future.succeededFuture(request.getId()) : groupService.getGroupByExternalId(request.getExternalId()).map(group -> group.getString("id"));
        groupIdFuture.onSuccess(groupId -> {
            // Create update data
            final JsonObject group = new JsonObject().put("id", groupId);

            if (request.getName() != null) {
                group.put("name", request.getName());
            }
            // classId is needed only when creating
            final String classId = "";
            // structureId is needed only when creating
            final String structureId = "";
            // Update group using existing service
            groupService.createOrUpdateManual(group, structureId, classId, event -> {
                if (event.isRight()) {
                    promise.complete(new UpdateGroupResponseDTO(true));
                } else {
                    log.error("Error updating group: {}", event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
        }).onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Deletes an existing manual group from the directory.
     *
     * @param request The request containing the ID of the group to be deleted
     * @return Response with the deletion result
     */
    @Override
    public Future<DeleteGroupResponseDTO> deleteManualGroup(DeleteGroupRequestDTO request) {
        final Promise<DeleteGroupResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for deleteManualGroup: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        // If groupExternalId is provided, fetch the group ID from the database
        final Future<String> groupIdFuture = request.getId() != null ? Future.succeededFuture(request.getId()) : groupService.getGroupByExternalId(request.getExternalId()).map(group -> group.getString("id"));

        groupIdFuture.onSuccess(groupId -> {
            // Delete group using existing service
            groupService.deleteManual(groupId, event -> {
                if (event.isRight()) {
                    promise.complete(new DeleteGroupResponseDTO(true));
                } else {
                    log.error("Error deleting group: {}", event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
        }).onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Adds a user as a member to an existing group.
     *
     * @param request The request containing group and user IDs
     * @return Response with the result of the addition
     */
    @Override
    public Future<AddGroupMemberResponseDTO> addGroupMember(AddGroupMemberRequestDTO request) {
        final Promise<AddGroupMemberResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for addGroupMember: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        final JsonArray userIds = new JsonArray().add(request.getUserId());
        // If groupExternalId is provided, fetch the group ID from the database
        final Future<String> groupIdFuture = request.getGroupId() != null ? Future.succeededFuture(request.getGroupId()) : groupService.getGroupByExternalId(request.getGroupExternalId()).map(group -> group.getString("id"));
        groupIdFuture.onSuccess(groupId -> groupService.addUsers(groupId, userIds, event -> {
            if(event.isRight()){
                promise.complete(new AddGroupMemberResponseDTO(true));
            }else{
                log.error("Error adding user to group: {}", event.left().getValue());
                promise.fail(event.left().getValue());
            }
        })).onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Removes a user from a group.
     *
     * @param request The request containing group and user IDs
     * @return Response with the result of the removal
     */
    @Override
    public Future<RemoveGroupMemberResponseDTO> removeGroupMember(RemoveGroupMemberRequestDTO request) {
        final Promise<RemoveGroupMemberResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for removeGroupMember: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }

        final JsonArray userIds = new JsonArray().add(request.getUserId());
        // If groupExternalId is provided, fetch the group ID from the database
        final Future<String> groupIdFuture = request.getGroupId() != null ? Future.succeededFuture(request.getGroupId()) : groupService.getGroupByExternalId(request.getGroupExternalId()).map(group -> group.getString("id"));
        groupIdFuture.onSuccess(groupId -> groupService.removeUsers(groupId, userIds, event -> {
            if(event.isRight()){
                promise.complete(new RemoveGroupMemberResponseDTO(true));
            }else{
                log.error("Error adding user to group: {}", event.left().getValue());
                promise.fail(event.left().getValue());
            }
        })).onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Finds a group by its external ID.
     *
     * @param request The request containing the external ID of the group
     * @return Response with the found group information
     */
    @Override
    public Future<FindGroupByExternalIdResponseDTO> findGroupByExternalId(FindGroupByExternalIdRequestDTO request) {
        final Promise<FindGroupByExternalIdResponseDTO> promise = Promise.promise();
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for findGroupByExternalId: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        // Find group by external ID using existing service
        groupService.getGroupByExternalId(request.getExternalId()).onSuccess(group -> {
            if (group != null) {
                final String id = group.getString("id");
                final String name = group.getString("name");
                promise.complete(new FindGroupByExternalIdResponseDTO(new GroupDTO(id, name)));
            } else {
                promise.fail("directory.group.notfound");
            }
        }).onFailure(error -> {
            log.error("Error finding group by external ID: ", error);
            promise.fail(error);
        });

        return promise.future();
    }

    /**
     * Retrieves display names for multiple users by their ENT IDs.
     *
     * @param request The request containing a list of user IDs
     * @return Response with a map of user IDs to their display names
     */
    @Override
    public Future<GetUserDisplayNamesResponseDTO> getUserDisplayNames(GetUserDisplayNamesRequestDTO request) {
        final Promise<GetUserDisplayNamesResponseDTO> promise = Promise.promise();
        
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for getUserDisplayNames: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        
        // Convert list of user IDs to JsonArray
        final JsonArray userIdsArray = new JsonArray(request.getUserIds());
        
        // Use the UserService to get display names
        userService.getUsersDisplayNames(userIdsArray)
            .onSuccess(result -> {
                // Convert JsonObject to Map<String, String>
                final Map<String, String> displayNamesMap = new HashMap<>();
                for (String userId : result.fieldNames()) {
                    displayNamesMap.put(userId, result.getString(userId));
                }
                
                // Return response DTO with the map
                promise.complete(new GetUserDisplayNamesResponseDTO(displayNamesMap));
            })
            .onFailure(error -> {
                log.error("Error retrieving user display names: ", error);
                promise.fail(error);
            });
        
        return promise.future();
    }

    /**
     * Retrieves users by their ENT IDs including profile and function information
     *
     * @param request The request containing a list of user IDs
     * @return Response with detailed user information
     */
    @Override
    public Future<GetUsersByIdsResponseDTO> getUsersByIds(GetUsersByIdsRequestDTO request) {
        final Promise<GetUsersByIdsResponseDTO> promise = Promise.promise();
        
        // Check if the request is valid
        if (request == null || !request.isValid()) {
            log.error("Invalid request for getUsersByIds: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }
        
        // Convert list of user IDs to JsonArray
        final JsonArray userIdsArray = new JsonArray(request.getUserIds());
        
        // Use the UserService to get users by IDs
        userService.getUsersByIds(userIdsArray)
            .onSuccess(usersArray -> {
                // Transform JsonArray of users to List<UserDTO>
                List<UserDTO> usersList = new ArrayList<>();
                
                for (int i = 0; i < usersArray.size(); i++) {
                    JsonObject userJson = usersArray.getJsonObject(i);
                    
                    String id = userJson.getString("id");
                    String displayName = userJson.getString("displayName");
                    
                    // Extract the first profile from the list or empty string if none
                    String profile = "";
                    JsonArray profilesArray = userJson.getJsonArray("profiles");
                    if (profilesArray != null && profilesArray.size() > 0) {
                        profile = profilesArray.getString(0);
                    }
                    
                    // Handle functions - convert Neo4j array format to map
                    Map<String, List<String>> functions = new HashMap<>();
                    JsonArray functionsArray = userJson.getJsonArray("functions");
                    if (functionsArray != null) {
                        for (int j = 0; j < functionsArray.size(); j++) {
                            JsonArray functionEntry = functionsArray.getJsonArray(j);
                            if (functionEntry != null && functionEntry.size() >= 2) {
                                String functionCode = functionEntry.getString(0);
                                if (functionCode != null) {
                                    // Extract scope from the second element
                                    List<String> scope = new ArrayList<>();
                                    Object scopeObj = functionEntry.getValue(1);
                                    
                                    if (scopeObj instanceof JsonArray) {
                                        JsonArray scopeArray = (JsonArray) scopeObj;
                                        for (int k = 0; k < scopeArray.size(); k++) {
                                            String scopeItem = scopeArray.getString(k);
                                            if (scopeItem != null) {
                                                scope.add(scopeItem);
                                            }
                                        }
                                    } else if (scopeObj instanceof String) {
                                        scope.add((String) scopeObj);
                                    }
                                    
                                    functions.put(functionCode, scope);
                                }
                            }
                        }
                    }
                    
                    usersList.add(new UserDTO(id, displayName, profile, functions));
                }
                
                // Return the response DTO with the list of users
                promise.complete(new GetUsersByIdsResponseDTO(usersList));
            })
            .onFailure(error -> {
                log.error("Error retrieving users by IDs: ", error);
                promise.fail(error);
            });

        return promise.future();
    }
}