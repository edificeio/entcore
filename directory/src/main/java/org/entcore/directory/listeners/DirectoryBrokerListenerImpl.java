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
import org.entcore.directory.services.impl.DefaultGroupService;


/**
 * Implementation of the DirectoryBrokerListener interface.
 * This class handles group management operations received through the message broker.
 */
public class DirectoryBrokerListenerImpl implements DirectoryBrokerListener {
    
    private static final Logger log = LoggerFactory.getLogger(DirectoryBrokerListenerImpl.class);
    private final GroupService groupService;

    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param vertx The Vertx instance
     */
    public DirectoryBrokerListenerImpl(Vertx vertx) {
        this(new DefaultGroupService(vertx.eventBus()));
    }

    /**
     * Constructor for DirectoryBrokerListenerImpl.
     *
     * @param groupService The group service to handle group operations
     */
    public DirectoryBrokerListenerImpl(GroupService groupService) {
        this.groupService = groupService;
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
}