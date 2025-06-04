package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.communication.*;

/**
 * This interface defines the methods that will be used to listen to events from the communication broker.
 * It provides functionalities for managing group communications through the ENT platform.
 */
public interface CommunicationBrokerListener {
  /**
   * Creates a communication link between two groups.
   * This allows members of each group to communicate with the members of the other group.
   *
   * @param request The request containing the source and target group IDs
   * @return A response indicating whether the link was created successfully
   */
  @BrokerListener(subject = "communication.link.groups.add", proxy = true)
  Future<AddLinkBetweenGroupsResponseDTO> addLinkBetweenGroups(AddLinkBetweenGroupsRequestDTO request);

  /**
   * Adds communication links between a group and its users.
   * This defines how group members can communicate with each other.
   *
   * @param request The request containing the group ID and direction of communication
   * @return A response indicating whether the links were added successfully
   */
  @BrokerListener(subject = "communication.link.users.add", proxy = true)
  Future<AddCommunicationLinksResponseDTO> addCommunicationLinks(AddCommunicationLinksRequestDTO request);

  /**
   * Removes communication links between a group and its users.
   *
   * @param request The request containing the group ID and direction of communication to remove
   * @return A response indicating whether the links were removed successfully
   */
  @BrokerListener(subject = "communication.link.users.remove", proxy = true)
  Future<RemoveCommunicationLinksResponseDTO> removeCommunicationLinks(RemoveCommunicationLinksRequestDTO request);

  /**
   * Recreates communication links between a group and its users.
   * This is useful when membership changes and links need to be updated.
   * It involves removing existing links and then adding them back.
   *
   * @param request The request containing the group ID and direction of communication
   * @return A response indicating whether the links were recreated successfully
   */
  @BrokerListener(subject = "communication.link.users.recreate", proxy = true)
  Future<RecreateCommunicationLinksResponseDTO> recreateCommunicationLinks(RecreateCommunicationLinksRequestDTO request);
}