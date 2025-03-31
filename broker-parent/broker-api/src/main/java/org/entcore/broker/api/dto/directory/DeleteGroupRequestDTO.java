package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents a request to delete a group in the directory.
 * It contains the ID and external ID of the group to be deleted.
 * The request is used to inform the server about the group that needs to be deleted.
 * It is typically used in conjunction with the DeleteGroupResponseDTO class, which contains the result of the deletion operation.
 */
public class DeleteGroupRequestDTO {
  /**
   * The ID of the group to be deleted.
   * It is a unique identifier for the group in the directory. It can be null if the groupExternalId is provided.
   */
  private final String id;
  /**
   * The external ID of the group to be deleted.
   * It is a unique identifier for the group in the directory. It can be null if the groupId is provided.
   */
  private final String externalId;

  @JsonCreator
  public DeleteGroupRequestDTO(@JsonProperty("id") String id, @JsonProperty("externalId") String externalId) {
    this.id = id;
    this.externalId = externalId;
  }

  /**
   * Gets the ID of the group to be deleted.
   * @return The group ID. It can be null if the groupExternalId is provided.
   * @see #getExternalId()
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the external ID of the group to be deleted.
   * @return The group external ID. It can be null if the groupId is provided.
   * @see #getId()
  */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Validates the request to ensure that at least one of id or externalId is provided.
   * @return true if the request is valid, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    if(StringUtils.isBlank(id) && StringUtils.isBlank(externalId)) {
      return false;
    }
    return true;
  }
  @Override
  public String toString() {
    return "DeleteGroupRequestDTO{" +
            "id='" + id + '\'' +
            ", externalId='" + externalId + '\'' +
            '}';
  }
}
