package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

import java.util.List;

/**
 * This class represents a request to upsert shares for a group.
 * It contains the group ID, group external ID, resource ID, application name, and permissions.
 */
public class UpsertGroupSharesRequestDTO {
  /**
   * The ID of the current user making the request.
   * It is a unique identifier for the user in the directory.
   */
  private final String currentUserId;
  /**
   * The ID of the group for which the shares will be upserted.
   * It is a unique identifier for the group in the directory. It can be null if the groupExternalId is provided.
   */
  private final String groupId;
  /**
   * The list of permissions to be granted to the group for the resource.
   * It is a list of strings representing the permissions.
   */
  private final List<String> permissions;
  /**
   * The ID of the resource for which the shares will be upserted.
   * It is a unique identifier for the resource.
   */
  private final String resourceId;
  /**
   * The name of the application containing the resource
   * It is a unique identifier for the application.
   */
  private final String application;

  @JsonCreator
  public UpsertGroupSharesRequestDTO(@JsonProperty("currentUserId") String currentUserId, @JsonProperty("application") String application, @JsonProperty("groupId") String groupId, @JsonProperty("resourceId") String resourceId, @JsonProperty("permissions") List<String> permissions) {
    this.currentUserId = currentUserId;
    this.groupId = groupId;
    this.permissions = permissions;
    this.resourceId = resourceId;
    this.application = application;
  }

  /**
   * Get the application name containing the resource.
   * @return The application name. It is a unique identifier for the application.
   */
  public String getApplication() {
    return application;
  }

  /**
   * Get the list of permissions to be granted to the group for the resource.
   * @return A list of permissions represented by strings.
   */
  public List<String> getPermissions() {
    return permissions;
  }

  /**
   * Get the ID of the resource for which the shares will be upserted.
   * @return The resource ID. It is a unique identifier for the resource.
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Get the ID of the group for which the shares will be upserted.
   * @return The group ID.
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Get the ID of the current user making the request.
   * @return The current user ID. It is a unique identifier for the user.
   */
  public String getCurrentUserId() {
    return currentUserId;
  }

  @Transient()
  public boolean isValid() {
    if(StringUtils.isEmpty(groupId)) {
      return false;
    }
    if(StringUtils.isEmpty(resourceId)) {
      return false;
    }
    if(permissions == null || permissions.isEmpty()) {
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
    return "UpsertGroupSharesRequestDTO{" +
            "currentUserId='" + currentUserId + '\'' +
            "groupId='" + groupId + '\'' +
            ", permissions=" + permissions +
            ", resourceId='" + resourceId + '\'' +
            ", application='" + application + '\'' +
            '}';
  }
}
