package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class represents a request to upsert shares for a group.
 * It contains the group ID, group external ID, resource ID, application name, and permissions.
 */
public class UpsertGroupSharesRequestDTO {
  /**
   * The ID of the group for which the shares will be upserted.
   * It is a unique identifier for the group in the directory. It can be null if the groupExternalId is provided.
   */
  private final String groupId;
  /**
   * The external ID of the group for which the shares will be upserted.
   * It is a unique identifier for the group in the directory. It can be null if the groupId is provided.
   */
  private final String groupExternalId;
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
  public UpsertGroupSharesRequestDTO(@JsonProperty("application") String application, @JsonProperty("groupId") String groupId, @JsonProperty("groupExternalId") String groupExternalId, @JsonProperty("resourceId") String resourceId, @JsonProperty("permissions") List<String> permissions) {
    this.groupId = groupId;
    this.groupExternalId = groupExternalId;
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
   * @return The group ID. It can be null if the groupExternalId is provided.
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Get the external ID of the group for which the shares will be upserted.
   * @return The group external ID. It can be null if the groupId is provided.
   */
  public String getGroupExternalId() {
    return groupExternalId;
  }
}
