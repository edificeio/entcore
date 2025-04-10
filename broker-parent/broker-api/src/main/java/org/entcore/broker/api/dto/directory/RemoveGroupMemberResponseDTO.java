

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * This class represents a response to a request to remove a member from a group in the directory.
 * It contains a boolean indicating whether the member was successfully removed or not.
 * The response is used to inform the client about the result of the removal operation.
 * It is typically used in conjunction with the RemoveGroupMemberRequestDTO class, which contains the details of the member to be removed.
 */
public class RemoveGroupMemberResponseDTO {
  /**
   * A boolean indicating whether the member was successfully removed from the group.
   * If true, the member was removed successfully. If false, the removal operation failed.
   */
  private final boolean removed;

  @JsonCreator
  public RemoveGroupMemberResponseDTO(@JsonProperty("removed") boolean removed) {
    this.removed = removed;
  }

  /**
   * Gets the boolean indicating whether the member was successfully removed from the group.
   * @return true if the member was removed successfully, false otherwise.
   */
  public boolean isRemoved() {
    return removed;
  }
}
