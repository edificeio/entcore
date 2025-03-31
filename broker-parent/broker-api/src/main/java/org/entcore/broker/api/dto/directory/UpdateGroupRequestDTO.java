package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to update an existing group in the directory.
 * It contains the ID, external ID, and name of the group to be updated.
 * The request is used to inform the server about the group that needs to be updated.
 * It is typically used in conjunction with the UpdateGroupResponseDTO class, which contains the result of the update operation.
 */
public class UpdateGroupRequestDTO {
  /**
   * The ID of the group to be updated.
   * It is a unique identifier for the group in the directory. It can be null if the externalId is provided.
   */
  private final String id;
  /**
   * The external ID of the group to be updated.
   * It is a unique identifier for the group in the directory. It can be null if the id is provided.
   */
  private final String externalId;
  /**
   * The name of the group to be updated.
   * It is a human-readable name for the group. It cannot be null or empty.
   */
  private final String name;

  @JsonCreator
  public UpdateGroupRequestDTO(@JsonProperty("id") String id, @JsonProperty("externalId") String externalId, @JsonProperty("name") String name) {
    this.id = id;
    this.externalId = externalId;
    this.name = name;
  }

  /**
   * Gets the ID of the group to be updated.
   * @return The group ID. It can be null if the externalId is provided.
   * @see #getExternalId()
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the external ID of the group to be updated.
   * @return The group external ID. It can be null if the id is provided.
   * @see #getId()
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Gets the name of the group to be updated.
   * @return The name of the group. It cannot be null or empty.
   */
  public String getName() {
    return name;
  }

  /**
   * Validates the request to ensure that at least one of id or externalId is provided.
   * @return true if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    if (StringUtils.isBlank(id) && StringUtils.isBlank(externalId)) {
      return false;
    }
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "UpdateGroupRequestDTO{" +
            "id='" + id + '\'' +
            ", externalId='" + externalId + '\'' +
            ", name='" + name + '\'' +
            '}';
  }
}
