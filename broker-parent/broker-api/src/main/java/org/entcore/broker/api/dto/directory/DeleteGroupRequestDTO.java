package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
}
