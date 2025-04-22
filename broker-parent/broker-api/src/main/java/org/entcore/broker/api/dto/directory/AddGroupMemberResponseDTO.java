

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * This class represents a response to a request to add a member to a group in the directory.
 * It contains a boolean indicating whether the member was successfully added or not.
 * The response is used to inform the client about the result of the addition operation.
 * It is typically used in conjunction with the AddGroupMemberRequestDTO class, which contains the details of the member to be added.
 */
public class AddGroupMemberResponseDTO {
  /**
   * A boolean indicating whether the member was successfully added to the group.
   * If true, the member was added successfully. If false, the addition operation failed.
   */
  private final boolean added;

  @JsonCreator
  public AddGroupMemberResponseDTO(@JsonProperty("added") boolean added) {
    this.added = added;
  }
  /**
   * Gets the boolean indicating whether the member was successfully added to the group.
   * @return true if the member was added successfully, false otherwise.
   */
  public boolean isAdded() {
    return added;
  }
}
