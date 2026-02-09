package org.entcore.directory.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.directory.*;
import org.entcore.broker.api.dto.directory.user.UserProfileDTOStructure;
import org.entcore.broker.proxy.DirectoryBrokerListener;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.ClassService;
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
    private final SchoolService structureService;
    private final ClassService classService;

    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param vertx The Vertx instance
     * @param userService The user service to handle user operations
     * @param structureService The structure service to handle structure operations
     */
    public DirectoryBrokerListenerImpl(Vertx vertx, UserService userService, SchoolService structureService, ClassService classService) {
        this(new DefaultGroupService(vertx.eventBus()), userService, structureService, classService);
    }
    
    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param groupService The group service to handle group operations
     * @param userService The user service to handle user operations
     */
    public DirectoryBrokerListenerImpl(GroupService groupService, UserService userService, SchoolService structureService, ClassService classService) {
        this.groupService = groupService;
        this.userService = userService;
        this.structureService = structureService;
        this.classService = classService;
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
        final String classId = request.getClassId() != null ? request.getClassId(): "";

        // Create group using existing service
        groupService.createOrUpdateManual(group, structureId, classId, event -> {
            if (event.isRight()) {
                final String id = event.right().getValue().getString("id");
                
                // Check if labels are provided in the request
                if (request.getLabels() != null && !request.getLabels().isEmpty()) {
                    // Add the labels to the created group
                    groupService.addLabelsToGroup(id, request.getLabels())
                        .onSuccess(labelResult -> {
                            log.debug("Successfully added labels {} to group {}", request.getLabels(), id);
                            promise.complete(new CreateGroupResponseDTO(id));
                        })
                        .onFailure(error -> {
                            log.error("Failed to add labels {} to group {}: {}", request.getLabels(), id, error.getMessage());
                            // We still consider the group creation as successful even if label addition fails
                            promise.complete(new CreateGroupResponseDTO(id));
                        });
                } else {
                    // No labels to add, complete directly
                    promise.complete(new CreateGroupResponseDTO(id));
                }
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

    /**
     * Retrieves user by its ENT ID with basic profile information, classes informations and hobbies
     *
     * @param request The request containing a user ID
     * @return Response with detailed user information
     */
    @Override
    public Future<GetClassAdminResponseDTO> getClassAdminUsers(GetClassAdminRequestDTO request) {
        final Promise<GetClassAdminResponseDTO> promise = Promise.promise();

        if (request == null || !request.isValid()) {
            log.error("Invalid request for getClassAdminUsers: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }

        String userId = request.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            promise.fail("request.parameters.userid.invalid");
            return promise.future();
        }

        this.userService.getUserInfos(request.getUserId(), result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {
                    promise.complete(new GetClassAdminResponseDTO(result.right().getValue()));
                } else {
                    log.warn("No user infos could be found for " + request.getUserId());
                    promise.fail(result.left().getValue());
                }
            } else {
                final String reason = result.left().getValue();
                log.warn("An error occurred while fetching user infos for " + request.getUserId() + " : " + reason);
                promise.fail(reason);
            }
        });
        return promise.future();
    }

    /**
     * Retrieves user linked to a class with basic profile information, classes informations, hobbies 
     * and INE + relatives if requested
     * with additional parameters to specify the type of users to retrieve
     *
     * @param request The request containing a user ID
     * @return Response with detailed user information
     */
    @Override
    public Future<GetUserInClassWithParamsResponseDTO> getUserInClassWithParams(GetUserInClassWithParamsRequestDTO request) {
        final Promise<GetUserInClassWithParamsResponseDTO> promise = Promise.promise();

        if (request == null || !request.isValid()) {
            log.error("Invalid request for getUserInClassWithParams: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }

        String classId = request.getClassId();
        String types = request.getType();
        boolean collectRelative = request.doesCollectRelative();
        boolean ine = request.doesCollectIne();
        if (classId == null || classId.trim().isEmpty()) {
            promise.fail("request.parameters.classid.invalid");
            return promise.future();
        }

        classService.findUsers(classId, types, collectRelative, ine, result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {
                    promise.complete(new GetUserInClassWithParamsResponseDTO(result.right().getValue()));
                } else {
                    log.warn("No user infos could be found in class " + request.getClassId());
                    promise.fail(result.left().getValue());
                }
            } else {
                final String reason = result.left().getValue();
                log.warn("An error occurred while fetching user infos in class " + request.getClassId() + " : " + reason);
                promise.fail(reason);
            }
        });
        return promise.future();
    }
    
    /**
     * Retrieves all users linked to a structure with basic profile information, classes informations and hobbies
     *
     * @param request The request containing a structure ID
     * @return Response with a list of detailed user information
     */
    @Override
    public Future<GetStructureUsersResponseDTO> getStructureUsers(GetStructureUsersRequestDTO request) {
        final Promise<GetStructureUsersResponseDTO> promise = Promise.promise();

        if (request == null || !request.isValid()) {
            log.error("Invalid request for getStructureUsers: {}", request);
            promise.fail("request.parameters.invalid");
            return promise.future();
        }

        String structureId = request.getStructureId();
        if (structureId == null || structureId.trim().isEmpty()) {
            promise.fail("request.parameters.structureid.invalid");
            return promise.future();
        }

        // The boolean params might be used to filter deleted users
        // TODO: update the method to find if the user is an admc or not (third params) 
        // As we do not have acces to the user informations in the broker
        // we default to false for now
        // In directory > src > ... > controllers > StructureController.java:
        // final boolean isAdmc = (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN));
        this.structureService.userList(request.getStructureId(), false, false, result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        JsonArray jsonArray = result.right().getValue();
                        List<UserProfileDTOStructure> userProfiles = objectMapper.readValue(
                            jsonArray.toString(),
                            new TypeReference<List<UserProfileDTOStructure>>() {}
                        );
                        promise.complete(new GetStructureUsersResponseDTO(userProfiles));
                    } catch (Exception e) {
                        log.error("Error converting JsonArray to List<UserProfileDTOStructure>", e);
                        promise.fail("Error processing user data");
                    }
                } else {
                    log.warn("No users found for structure " + request.getStructureId());
                    promise.fail("No users found for structure");
                }
            } else {
                final String reason = result.left().getValue();
                log.warn("An error occurred while fetching structure " + request.getStructureId() + " : " + reason);
                promise.fail(reason);
            }
        });
        return promise.future();
    }
}