package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a request to create a communication link between two groups.
 * It contains the IDs of the source and target groups.
 */
public class AddLinkBetweenGroupsRequestDTO {
  
  /**
   * The ID of the source group for the communication link.
   */
  private final String startGroupId;
  
  /**
   * The ID of the target group for the communication link.
   */
  private final String endGroupId;

  /**
   * Creates a new request to add a communication link between two groups.
   *
   * @param startGroupId The ID of the source group
   * @param endGroupId The ID of the target group
   */
  @JsonCreator
  public AddLinkBetweenGroupsRequestDTO(
      @JsonProperty("startGroupId") String startGroupId,
      @JsonProperty("endGroupId") String endGroupId) {
    this.startGroupId = startGroupId;
    this.endGroupId = endGroupId;
  }
  
  /**
   * Validates that the required fields for this request are present.
   * Both source and target group IDs must be provided.
   * 
   * @return true if the request is valid, false otherwise
   */
  public boolean isValid() {
    return !StringUtils.isBlank(startGroupId) && !StringUtils.isBlank(endGroupId);
  }
  
  /**
   * Gets the ID of the source group.
   * @return The source group ID
   */
  public String getStartGroupId() {
    return startGroupId;
  }
  
  /**
   * Gets the ID of the target group.
   * @return The target group ID
   */
  public String getEndGroupId() {
    return endGroupId;
  }
  
  @Override
  public String toString() {
    return "AddLinkBetweenGroupsRequestDTO{" +
        "startGroupId='" + startGroupId + '\'' +
        ", endGroupId='" + endGroupId + '\'' +
        '}';
  }
}