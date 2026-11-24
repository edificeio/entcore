package org.entcore.directory.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.edifice.events.vertx.BrokerResponseCodes;
import io.edifice.directory.events.DirectoryBrokerListener;
import io.edifice.directory.v1.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultGroupService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of the DirectoryBrokerListener.
 * This class handles directory operations received through the message broker.
 */
public class DirectoryBrokerListenerImpl extends DirectoryBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(DirectoryBrokerListenerImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
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
     * @param classService The class service to handle class operations
     */
    public DirectoryBrokerListenerImpl(Vertx vertx, UserService userService, SchoolService structureService, ClassService classService) {
        this(vertx, new DefaultGroupService(vertx.eventBus()), userService, structureService, classService);
    }

    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param vertx The Vertx instance
     * @param groupService The group service to handle group operations
     * @param userService The user service to handle user operations
     * @param structureService The structure service to handle structure operations
     * @param classService The class service to handle class operations
     */
    public DirectoryBrokerListenerImpl(Vertx vertx, GroupService groupService, UserService userService, SchoolService structureService, ClassService classService) {
        super(vertx);
        this.groupService = groupService;
        this.userService = userService;
        this.structureService = structureService;
        this.classService = classService;
    }

    /**
     * @return a failure with the code 400 for a request whose parameters are invalid
     */
    private static <T> Future<T> invalidRequest(final String method, final Object request) {
        log.error("Invalid request for " + method + ": " + request);
        return Future.failedFuture(new IllegalArgumentException(BrokerResponseCodes.INVALID_PARAMETERS));
    }

    /**
     * @return the identifier of the group : the one of the request when it is given, otherwise the one of the group
     * that has the external identifier of the request
     */
    private Future<String> getGroupId(final String groupId, final String groupExternalId) {
        if (StringUtils.isNotBlank(groupId)) {
            return Future.succeededFuture(groupId);
        }
        return groupService.getGroupByExternalId(groupExternalId).map(group -> group.getString("id"));
    }

    /**
     * Creates a manual group in the directory.
     *
     * @param request The request containing group details
     * @return Response with the created group information
     */
    @Override
    protected Future<CreateGroupResponse> onCreateManualGroup(CreateGroupRequest request) {
        if (StringUtils.isBlank(request.getName())) {
            return invalidRequest("createManualGroup", request);
        }
        final Promise<CreateGroupResponse> promise = Promise.promise();
        // Create group data
        final JsonObject group = new JsonObject()
                .put("name", request.getName())
                .put("filter", request.getFilter())
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
                final CreateGroupResponse response = new CreateGroupResponseBuilder().withId(id).build();
                // Check if labels are provided in the request
                if (request.getLabels() != null && !request.getLabels().isEmpty()) {
                    // Add the labels to the created group
                    groupService.addLabelsToGroup(id, request.getLabels())
                        .onSuccess(labelResult -> {
                            log.debug("Successfully added labels " + request.getLabels() + " to group " + id);
                            promise.complete(response);
                        })
                        .onFailure(error -> {
                            log.error("Failed to add labels " + request.getLabels() + " to group " + id + ": " + error.getMessage());
                            // We still consider the group creation as successful even if label addition fails
                            promise.complete(response);
                        });
                } else {
                    // No labels to add, complete directly
                    promise.complete(response);
                }
            } else {
                log.error("Error creating group: " + event.left().getValue());
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
    protected Future<UpdateGroupResponse> onUpdateManualGroup(UpdateGroupRequest request) {
        if ((StringUtils.isBlank(request.getId()) && StringUtils.isBlank(request.getExternalId())) || StringUtils.isBlank(request.getName())) {
            return invalidRequest("updateManualGroup", request);
        }
        return getGroupId(request.getId(), request.getExternalId()).compose(groupId -> {
            final Promise<UpdateGroupResponse> promise = Promise.promise();
            // Create update data
            final JsonObject group = new JsonObject().put("id", groupId).put("name", request.getName());
            // classId is needed only when creating
            final String classId = "";
            // structureId is needed only when creating
            final String structureId = "";
            // Update group using existing service
            groupService.createOrUpdateManual(group, structureId, classId, event -> {
                if (event.isRight()) {
                    promise.complete(new UpdateGroupResponseBuilder().withUpdated(true).build());
                } else {
                    log.error("Error updating group: " + event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
            return promise.future();
        });
    }

    /**
     * Deletes an existing manual group from the directory.
     *
     * @param request The request containing the ID of the group to be deleted
     * @return Response with the deletion result
     */
    @Override
    protected Future<DeleteGroupResponse> onDeleteManualGroup(DeleteGroupRequest request) {
        if (StringUtils.isBlank(request.getId()) && StringUtils.isBlank(request.getExternalId())) {
            return invalidRequest("deleteManualGroup", request);
        }
        return getGroupId(request.getId(), request.getExternalId()).compose(groupId -> {
            final Promise<DeleteGroupResponse> promise = Promise.promise();
            // Delete group using existing service
            groupService.deleteManual(groupId, event -> {
                if (event.isRight()) {
                    promise.complete(new DeleteGroupResponseBuilder().withDeleted(true).build());
                } else {
                    log.error("Error deleting group: " + event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
            return promise.future();
        });
    }

    /**
     * Adds a user as a member to an existing group.
     *
     * @param request The request containing group and user IDs
     * @return Response with the result of the addition
     */
    @Override
    protected Future<AddGroupMemberResponse> onAddGroupMember(AddGroupMemberRequest request) {
        if (StringUtils.isBlank(request.getUserId()) || (StringUtils.isBlank(request.getGroupId()) && StringUtils.isBlank(request.getGroupExternalId()))) {
            return invalidRequest("addGroupMember", request);
        }
        final JsonArray userIds = new JsonArray().add(request.getUserId());
        return getGroupId(request.getGroupId(), request.getGroupExternalId()).compose(groupId -> {
            final Promise<AddGroupMemberResponse> promise = Promise.promise();
            groupService.addUsers(groupId, userIds, event -> {
                if (event.isRight()) {
                    promise.complete(new AddGroupMemberResponseBuilder().withAdded(true).build());
                } else {
                    log.error("Error adding user to group: " + event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
            return promise.future();
        });
    }

    /**
     * Removes a user from a group.
     *
     * @param request The request containing group and user IDs
     * @return Response with the result of the removal
     */
    @Override
    protected Future<RemoveGroupMemberResponse> onRemoveGroupMember(RemoveGroupMemberRequest request) {
        if (StringUtils.isBlank(request.getUserId()) || (StringUtils.isBlank(request.getGroupId()) && StringUtils.isBlank(request.getGroupExternalId()))) {
            return invalidRequest("removeGroupMember", request);
        }
        final JsonArray userIds = new JsonArray().add(request.getUserId());
        return getGroupId(request.getGroupId(), request.getGroupExternalId()).compose(groupId -> {
            final Promise<RemoveGroupMemberResponse> promise = Promise.promise();
            groupService.removeUsers(groupId, userIds, event -> {
                if (event.isRight()) {
                    promise.complete(new RemoveGroupMemberResponseBuilder().withRemoved(true).build());
                } else {
                    log.error("Error removing user from group: " + event.left().getValue());
                    promise.fail(event.left().getValue());
                }
            });
            return promise.future();
        });
    }

    /**
     * Finds a group by its external ID.
     *
     * @param request The request containing the external ID of the group
     * @return Response with the found group information
     */
    @Override
    protected Future<FindGroupByExternalIdResponse> onFindGroupByExternalId(FindGroupByExternalIdRequest request) {
        if (StringUtils.isBlank(request.getExternalId())) {
            return invalidRequest("findGroupByExternalId", request);
        }
        return groupService.getGroupByExternalId(request.getExternalId())
            .compose(group -> {
                if (group == null) {
                    return Future.<FindGroupByExternalIdResponse>failedFuture("directory.group.notfound");
                }
                return Future.succeededFuture(new FindGroupByExternalIdResponseBuilder().withGroup(new Group(group.getString("id"), group.getString("name"))).build());
            })
            .onFailure(error -> log.error("Error finding group by external ID: ", error));
    }

    /**
     * Retrieves display names for multiple users by their ENT IDs.
     *
     * @param request The request containing a list of user IDs
     * @return Response with a map of user IDs to their display names
     */
    @Override
    protected Future<GetUserDisplayNamesResponse> onGetUserDisplayNames(GetUserDisplayNamesRequest request) {
        if (isEmpty(request.getUserIds())) {
            return invalidRequest("getUserDisplayNames", request);
        }
        return userService.getUsersDisplayNames(new JsonArray(request.getUserIds()))
            .map(result -> {
                // Convert JsonObject to Map<String, String>
                final Map<String, String> displayNamesMap = new HashMap<>();
                for (String userId : result.fieldNames()) {
                    displayNamesMap.put(userId, result.getString(userId));
                }
                return new GetUserDisplayNamesResponseBuilder().withUserDisplayNames(displayNamesMap).build();
            })
            .onFailure(error -> log.error("Error retrieving user display names: ", error));
    }

    /**
     * Retrieves users by their ENT IDs including profile and function information, and the optional fields asked for
     *
     * @param request The request containing a list of user IDs and the optional fields to return
     * @return Response with detailed user information
     */
    @Override
    protected Future<GetUsersByIdsResponse> onGetUsersByIds(GetUsersByIdsRequest request) {
        if (isEmpty(request.getUserIds())) {
            return invalidRequest("getUsersByIds", request);
        }
        final Set<UserField> fields = toEnumSet(request.getFields(), UserField.class);
        return userService.getUsersByIds(new JsonArray(request.getUserIds()), fields)
            .map(usersArray -> {
                final List<User> usersList = new ArrayList<>();
                for (int i = 0; i < usersArray.size(); i++) {
                    usersList.add(toUser(usersArray.getJsonObject(i), fields));
                }
                return new GetUsersByIdsResponseBuilder().withUsers(usersList).build();
            })
            .onFailure(error -> log.error("Error retrieving users by IDs: ", error));
    }

    /**
     * @param userJson a user returned by {@link UserService#getUsersByIds(JsonArray, Set)}
     * @param fields the optional fields that were asked for
     * @return the user, with only the optional fields that were asked for
     */
    static User toUser(final JsonObject userJson, final Set<UserField> fields) {
        // Extract the first profile from the list or empty string if none
        String profile = "";
        final JsonArray profilesArray = userJson.getJsonArray("profiles");
        if (profilesArray != null && profilesArray.size() > 0) {
            profile = profilesArray.getString(0);
        }
        final UserBuilder builder = new UserBuilder()
            .withId(userJson.getString("id"))
            .withDisplayName(userJson.getString("displayName"))
            .withProfile(profile)
            .withFunctions(toFunctions(userJson.getJsonArray("functions")));
        if (fields.contains(UserField.firstName)) {
            builder.withFirstName(userJson.getString("firstName"));
        }
        if (fields.contains(UserField.lastName)) {
            builder.withLastName(userJson.getString("lastName"));
        }
        if (fields.contains(UserField.login)) {
            builder.withLogin(userJson.getString("login"));
        }
        if (fields.contains(UserField.email)) {
            builder.withEmail(userJson.getString("email"));
        }
        return builder.build();
    }

    /**
     * @param functionsArray the functions as Neo4j returns them : a list of [function code, scope]
     * @return the scopes of the functions, by function code
     */
    private static Map<String, List<String>> toFunctions(final JsonArray functionsArray) {
        final Map<String, List<String>> functions = new HashMap<>();
        if (functionsArray == null) {
            return functions;
        }
        for (int j = 0; j < functionsArray.size(); j++) {
            final JsonArray functionEntry = functionsArray.getJsonArray(j);
            if (functionEntry != null && functionEntry.size() >= 2) {
                final String functionCode = functionEntry.getString(0);
                if (functionCode != null) {
                    // Extract scope from the second element
                    final List<String> scope = new ArrayList<>();
                    final Object scopeObj = functionEntry.getValue(1);
                    if (scopeObj instanceof JsonArray) {
                        final JsonArray scopeArray = (JsonArray) scopeObj;
                        for (int k = 0; k < scopeArray.size(); k++) {
                            final String scopeItem = scopeArray.getString(k);
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
        return functions;
    }

    /**
     * Retrieves user by its ENT ID with basic profile information, classes informations and hobbies
     *
     * @param request The request containing a user ID
     * @return Response with detailed user information
     */
    @Override
    protected Future<GetClassAdminResponse> onGetClassAdminUsers(GetClassAdminRequest request) {
        if (StringUtils.isBlank(request.getUserId())) {
            return invalidRequest("getClassAdminUsers", request);
        }
        final Promise<GetClassAdminResponse> promise = Promise.promise();
        this.userService.getUserInfos(request.getUserId(), toEnumSet(request.getIncludes(), ClassIncludeField.class), result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {
                    try {
                        final JsonObject userInfoJson = result.right().getValue();
                        final UserProfileClassAdmin userProfile = objectMapper.readValue(
                            userInfoJson.toString(),
                            UserProfileClassAdmin.class
                        );
                        promise.complete(new GetClassAdminResponseBuilder().withData(userProfile).build());
                    } catch (Exception e) {
                        log.error("Error converting JsonObject to UserProfileClassAdmin", e);
                        promise.fail("Error processing user data");
                    }
                } else {
                    log.warn("No user infos could be found for " + request.getUserId());
                    promise.fail("No user found");
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
     * @param request The request containing a class ID
     * @return Response with detailed user information
     */
    @Override
    protected Future<GetUserInClassWithParamsResponse> onGetUserInClassWithParams(GetUserInClassWithParamsRequest request) {
        if (StringUtils.isBlank(request.getClassId()) || StringUtils.isBlank(request.getType())) {
            return invalidRequest("getUserInClassWithParams", request);
        }
        final Promise<GetUserInClassWithParamsResponse> promise = Promise.promise();
        final JsonArray types = new JsonArray().add(request.getType());
        classService.findUsers(request.getClassId(), types, request.isCollectRelative(), request.isIne(), result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {
                    try {
                        final JsonArray jsonArray = result.right().getValue();
                        final List<UserProfileInClass> userProfiles = objectMapper.readValue(
                            jsonArray.toString(),
                            new TypeReference<List<UserProfileInClass>>() {}
                        );
                        promise.complete(new GetUserInClassWithParamsResponseBuilder().withData(userProfiles).build());
                    } catch (Exception e) {
                        log.error("Error converting JsonArray to List<UserProfileInClass>", e);
                        promise.fail("Error processing user data");
                    }
                } else {
                    log.warn("No user infos could be found in class " + request.getClassId());
                    promise.fail("No users found in class");
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
    protected Future<GetStructureUsersResponse> onGetStructureUsers(GetStructureUsersRequest request) {
        if (StringUtils.isBlank(request.getStructureId())) {
            return invalidRequest("getStructureUsers", request);
        }
        final Promise<GetStructureUsersResponse> promise = Promise.promise();
        // The boolean params might be used to filter deleted users
        // TODO: update the method to find if the user is an admc or not (third params)
        // As we do not have acces to the user informations in the broker
        // we default to false for now
        // In directory > src > ... > controllers > StructureController.java:
        // final boolean isAdmc = (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN));
        this.structureService.userList(request.getStructureId(), false, false, result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && !result.right().getValue().isEmpty()) {
                    try {
                        final JsonArray jsonArray = result.right().getValue();
                        final List<UserProfileStructure> userProfiles = objectMapper.readValue(
                            jsonArray.toString(),
                            new TypeReference<List<UserProfileStructure>>() {}
                        );
                        promise.complete(new GetStructureUsersResponseBuilder().withData(userProfiles).build());
                    } catch (Exception e) {
                        log.error("Error converting JsonArray to List<UserProfileStructure>", e);
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

    /**
     * Retrieves the users of groups.
     *
     * @param request The request containing the list of group IDs
     * @return Response with the users of the groups, without their functions
     */
    @Override
    protected Future<GetUsersFromGroupsResponse> onGetUsersFromGroups(GetUsersFromGroupsRequest request) {
        if (isEmpty(request.getGroupIds())) {
            return invalidRequest("getUsersFromGroups", request);
        }
        final Promise<GetUsersFromGroupsResponse> promise = Promise.promise();
        userService.list(new JsonArray(request.getGroupIds()), new JsonArray(), true, null, event -> {
            if (event.isRight()) {
                final JsonArray usersArray = event.right().getValue();
                try {
                    final List<User> usersList = new ArrayList<>();
                    for (int i = 0; i < usersArray.size(); i++) {
                        final JsonObject userJson = usersArray.getJsonObject(i);
                        usersList.add(new UserBuilder()
                            .withId(userJson.getString("id"))
                            .withDisplayName(userJson.getString("username"))
                            .withProfile(userJson.getString("type"))
                            // functions are not returned by the query inside "userService.list" : use empty map
                            .withFunctions(new HashMap<>())
                            .build());
                    }
                    promise.complete(new GetUsersFromGroupsResponseBuilder().withUsers(usersList).build());
                } catch (Exception err) {
                    log.error("Error while parsing user array", err);
                    promise.fail(err);
                }
            } else {
                log.error("Error getting users from groups: " + event.left().getValue());
                promise.fail(event.left().getValue());
            }
        });
        return promise.future();
    }

    /**
     * Retrieves groups by their ENT IDs with basic information (id, name).
     * Unlike communication-rights-filtered lookups, this returns every group
     * found regardless of the caller's communication rights or structure
     * attachment.
     *
     * @param request The request containing the list of group IDs
     * @return Response with the found groups
     */
    @Override
    protected Future<GetGroupsByIdsResponse> onGetGroupsByIds(GetGroupsByIdsRequest request) {
        if (isEmpty(request.getGroupIds())) {
            return invalidRequest("getGroupsByIds", request);
        }
        final Promise<GetGroupsByIdsResponse> promise = Promise.promise();
        groupService.getBatchInfos(new JsonArray(request.getGroupIds()), GroupService.Field.DISPLAY_NAME.value(), event -> {
            if (event.isRight()) {
                final JsonArray groupsArray = event.right().getValue();
                try {
                    final List<Group> groupsList = new ArrayList<>();
                    for (int i = 0; i < groupsArray.size(); i++) {
                        final JsonObject groupJson = groupsArray.getJsonObject(i);
                        final String displayName = groupJson.getString("displayName", groupJson.getString("name"));
                        groupsList.add(new Group(groupJson.getString("id"), displayName));
                    }
                    promise.complete(new GetGroupsByIdsResponseBuilder().withGroups(groupsList).build());
                } catch (Exception err) {
                    log.error("Error while parsing group array", err);
                    promise.fail(err);
                }
            } else {
                log.error("Error getting groups by IDs: " + event.left().getValue());
                promise.fail(event.left().getValue());
            }
        });
        return promise.future();
    }

    /**
     * Not served by the directory : the callers receive an error instead of waiting for a timeout.
     */
    @Override
    protected Future<SearchStructuresResponse> onSearchStructures(SearchStructuresRequest request) {
        return Future.failedFuture(new UnsupportedOperationException("directory.structures.search is not implemented"));
    }

    private static boolean isEmpty(final Collection<?> values) {
        return values == null || values.isEmpty();
    }

    /**
     * @return the values, without the {@code null} ones (values of the enum that this version does not know)
     */
    static <E extends Enum<E>> Set<E> toEnumSet(final Collection<E> values, final Class<E> type) {
        final Set<E> set = EnumSet.noneOf(type);
        if (values != null) {
            values.stream().filter(Objects::nonNull).forEach(set::add);
        }
        return set;
    }
}
