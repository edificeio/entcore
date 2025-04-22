package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a request to remove shares from a group.
 * It contains the group ID, group external ID, resource ID, and application name.
 */
public class RemoveGroupSharesRequestDTO {
  /**
   * The ID of the current user making the request.
   * It is a unique identifier for the user in the directory.
   */
  private final String currentUserId;
  /**
   * The ID of the group from which the shares will be removed.
   * It is a unique identifier for the group in the directory. It can be null if the groupExternalId is provided.
   */
  private final String groupId;
  /**
   * The external ID of the group from which the shares will be removed.
   * It is a unique identifier for the group in the directory. It can be null if the groupId is provided.
   */
  private final String groupExternalId;
  /**
   * The ID of the resource from which the shares will be removed.
   * It is a unique identifier for the resource in the directory.
   */
  private final String resourceId;
  /**
   * The name of the application from which the shares will be removed.
   * It is a unique identifier for the application in the directory.
   */
  private final String application;

  @JsonCreator
  public RemoveGroupSharesRequestDTO(@JsonProperty("currentUserId") String currentUserId,@JsonProperty("application") String application, @JsonProperty("groupId") String groupId, @JsonProperty("groupExternalId") String groupExternalId, @JsonProperty("resourceId") String resourceId) {
    this.currentUserId = currentUserId;
    this.application = application;
    this.groupId = groupId;
    this.groupExternalId = groupExternalId;
    this.resourceId = resourceId;
  }

  /**
   * Gets the ID of the group from which the shares will be removed.
   * @return The group ID. It can be null if the groupExternalId is provided.
   * @see #getGroupExternalId()
   */
  public String getApplication() {
    return application;
  }

  /**
   * Gets the external ID of the group from which the shares will be removed.
   * @return The group external ID. It can be null if the groupId is provided.
   * @see #getGroupId()
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Gets the ID of the group from which the shares will be removed.
   * @return The group ID. It can be null if the groupExternalId is provided.
   * @see #getGroupExternalId()
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Gets the external ID of the group from which the shares will be removed.
   * @return The group external ID. It can be null if the groupId is provided.
   * @see #getGroupId()
   */
  public String getGroupExternalId() {
    return groupExternalId;
  }
  /**
   * Gets the ID of the current user making the request.
   * @return The current user ID. It is a unique identifier for the user in the directory.
   */
  public String getCurrentUserId() {
    return currentUserId;
  }

  public boolean isValid() {
    if(StringUtils.isEmpty(groupId)) {
      return false;
    }
    if(StringUtils.isEmpty(resourceId)) {
      return false;
    }
    if(StringUtils.isEmpty(application)) {
      return false;
    }
    if(StringUtils.isEmpty(currentUserId)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "RemoveGroupSharesRequestDTO{" +
            "currentUserId='" + currentUserId + '\'' +
            ", groupId='" + groupId + '\'' +
            ", groupExternalId='" + groupExternalId + '\'' +
            ", resourceId='" + resourceId + '\'' +
            ", application='" + application + '\'' +
            '}';
  }
}
