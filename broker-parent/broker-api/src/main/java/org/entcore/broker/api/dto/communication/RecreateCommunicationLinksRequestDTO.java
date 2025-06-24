package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to recreate communication links between a group and its users.
 * It involves first removing existing links and then adding them back.
 * This is useful when the membership of a group has changed.
 */
public class RecreateCommunicationLinksRequestDTO {
  
  /**
   * The ID of the group for which to recreate communication links.
   */
  private final String groupId;
  
  /**
   * The direction of communication for the links to be recreated.
   * Possible values: "INCOMING", "OUTGOING", "BOTH", "NONE"
   */
  private final String direction;

  /**
   * Creates a new request to recreate communication links for a group.
   *
   * @param groupId The ID of the group
   * @param direction The direction of communication for the links
   */
  @JsonCreator
  public RecreateCommunicationLinksRequestDTO(
      @JsonProperty("groupId") String groupId,
      @JsonProperty("direction") String direction) {
    this.groupId = groupId;
    this.direction = direction != null ? direction : "BOTH";
  }
  
  /**
   * Validates that the required fields for this request are present.
   * Group ID must be provided.
   * 
   * @return true if the request is valid, false otherwise
   */
  @Transient()
  public boolean isValid() {
    return !StringUtils.isBlank(groupId);
  }
  
  /**
   * Gets the ID of the group.
   * @return The group ID
   */
  public String getGroupId() {
    return groupId;
  }
  
  /**
   * Gets the direction of communication for the links.
   * @return The direction as a string
   */
  public String getDirection() {
    return direction;
  }
  
  @Override
  public String toString() {
    return "RecreateCommunicationLinksRequestDTO{" +
        "groupId='" + groupId + '\'' +
        ", direction='" + direction + '\'' +
        '}';
  }
}