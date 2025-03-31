

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to update a group in the directory.
 * It contains a boolean indicating whether the group was successfully updated or not.
 * The response is used to inform the client about the result of the update operation.
 */
public class UpdateGroupResponseDTO {
  /**
   * A boolean indicating whether the group was successfully updated.
   * If true, the group was updated successfully. If false, the update operation failed.
   */
  private final boolean updated;

  @JsonCreator
  public UpdateGroupResponseDTO(@JsonProperty("updated") boolean updated) {
    this.updated = updated;
  }

  /**
   * Gets the boolean indicating whether the group was successfully updated.
   * @return true if the group was updated successfully, false otherwise.
   */
  public boolean isUpdated() {
    return updated;
  }
}
