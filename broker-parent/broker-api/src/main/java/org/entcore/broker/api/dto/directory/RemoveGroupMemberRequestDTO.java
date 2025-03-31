package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to remove a member from a group in the directory.
 * It contains the group ID, group external ID, and user ID of the member to be removed.
 * The request is used to inform the server about the member that needs to be removed from the group.
 * It is typically used in conjunction with the RemoveGroupMemberResponseDTO class, which contains the result of the removal operation.
 */
public class RemoveGroupMemberRequestDTO {
  /**
   * The ID of the group from which the member will be removed.
   * It is a unique identifier for the group in the directory. It can be null if the groupExternalId is provided.
   */
  private final String groupId;
  /**
   * The external ID of the group from which the member will be removed.
   * It is a unique identifier for the group in the directory. It can be null if the groupId is provided.
   */
  private final String groupExternalId;
  /**
   * The ID of the user to be removed as a member of the group.
   * It is a unique identifier for the user in the directory.
   */
  private final String userId;

  @JsonCreator
  public RemoveGroupMemberRequestDTO(@JsonProperty("groupId") String groupId, @JsonProperty("groupExternalId") String groupExternalId, @JsonProperty("userId") String userId) {
    this.groupId = groupId;
    this.groupExternalId = groupExternalId;
    this.userId = userId;
  }

  /**
   * Gets the ID of the group from which the member will be removed.
   * @return The group ID. It can be null if the groupExternalId is provided.
   * @see #getGroupExternalId()
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Gets the external ID of the group from which the member will be removed.
   * @return The group external ID. It can be null if the groupId is provided.
   * @see #getGroupId()
   */
  public String getGroupExternalId() {
    return groupExternalId;
  }

  /**
   * Gets the ID of the user to be removed as a member of the group.
   * @return The user ID. It is a unique identifier for the user in the directory.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Validates the request to ensure that at least one of groupId or groupExternalId is provided, and userId is not blank.
   * @return true if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    if (StringUtils.isBlank(userId)) {
      return false;
    }
    if (StringUtils.isBlank(groupId) && StringUtils.isBlank(groupExternalId)) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "RemoveGroupMemberRequestDTO{" +
            "groupId='" + groupId + '\'' +
            ", groupExternalId='" + groupExternalId + '\'' +
            ", userId='" + userId + '\'' +
            '}';
  }
}
