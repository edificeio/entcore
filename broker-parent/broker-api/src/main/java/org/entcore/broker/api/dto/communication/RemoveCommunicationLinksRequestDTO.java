package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to remove communication links between a group and its users.
 * It contains the group ID and the direction of communication to remove.
 */
public class RemoveCommunicationLinksRequestDTO {
  
  /**
   * The ID of the group for which to remove communication links.
   */
  private final String groupId;
  
  /**
   * The direction of communication to remove.
   * Possible values: "INCOMING", "OUTGOING", "BOTH", "NONE"
   */
  private final String direction;

  /**
   * Creates a new request to remove communication links for a group.
   *
   * @param groupId The ID of the group
   * @param direction The direction of communication to remove
   */
  @JsonCreator
  public RemoveCommunicationLinksRequestDTO(
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
   * Gets the direction of communication to remove.
   * @return The direction as a string
   */
  public String getDirection() {
    return direction;
  }
  
  @Override
  public String toString() {
    return "RemoveCommunicationLinksRequestDTO{" +
        "groupId='" + groupId + '\'' +
        ", direction='" + direction + '\'' +
        '}';
  }
}