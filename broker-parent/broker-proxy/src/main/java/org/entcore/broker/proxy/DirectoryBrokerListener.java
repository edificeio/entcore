package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.directory.*;
/**
 * This interface defines the methods that will be used to listen to events from the directory broker.
 */
public interface DirectoryBrokerListener {
  /**
   * This method is used to create a new manual group in the directory.
   * @param request The request object containing the details of the group to be created.
   * @return A response object containing the details of the created group.
   */
  @BrokerListener(subject = "directory.group.manual.create", proxy = true)
  Future<CreateGroupResponseDTO> createManualGroup(CreateGroupRequestDTO request);

  /**
   * This method is used to update an existing manual group in the directory.
   * @param request The request object containing the details of the group to be updated.
   * @return A response object containing the details of the updated group.
   */
  @BrokerListener(subject = "directory.group.manual.update", proxy = true)
  Future<UpdateGroupResponseDTO> updateManualGroup(UpdateGroupRequestDTO request);

  /**
   * This method is used to delete an existing manual group in the directory.
   * @param request The request object containing the details of the group to be deleted.
   * @return A response object indicating the result of the deletion operation.
   */
  @BrokerListener(subject = "directory.group.manual.delete", proxy = true)
  Future<DeleteGroupResponseDTO> deleteManualGroup(DeleteGroupRequestDTO request);

  /**
   * This method is used to add a member to a group in the directory.
   * @param request The request object containing the details of the member to be added.
   * @return A response object indicating the result of the addition operation.
   */
  @BrokerListener(subject = "directory.group.member.add", proxy = true)
  Future<AddGroupMemberResponseDTO> addGroupMember(AddGroupMemberRequestDTO request);

  /**
   * This method is used to remove a member from a group in the directory.
   * @param request The request object containing the details of the member to be removed.
   * @return A response object indicating the result of the removal operation.
   */
  @BrokerListener(subject = "directory.group.member.delete", proxy = true)
  Future<RemoveGroupMemberResponseDTO> removeGroupMember(RemoveGroupMemberRequestDTO request);

  /**
   * This method is used to find a group by its external ID in the directory.
   * @param request The request object containing the external ID of the group to be found.
   * @return A response object containing the details of the found group or an indication that the group was not found.
   */
  @BrokerListener(subject = "directory.group.find.byexternalid", proxy = true)
  Future<FindGroupByExternalIdResponseDTO> findGroupByExternalId(FindGroupByExternalIdRequestDTO request);
}
