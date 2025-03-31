

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * This class represents a response to a request to delete a group in the directory.
 * It contains a boolean indicating whether the group was successfully deleted or not.
 * The response is used to inform the client about the result of the deletion operation.
 * It is typically used in conjunction with the DeleteGroupRequestDTO class, which contains the details of the group to be deleted.
 */
public class DeleteGroupResponseDTO {
  /**
   * A boolean indicating whether the group was successfully deleted.
   * If true, the group was deleted successfully. If false, the deletion operation failed.
   */
  private final boolean deleted;

  @JsonCreator
  public DeleteGroupResponseDTO(@JsonProperty("deleted") boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Gets the boolean indicating whether the group was successfully deleted.
   * @return true if the group was deleted successfully, false otherwise.
   */
  public boolean isDeleted() {
    return deleted;
  }
}
