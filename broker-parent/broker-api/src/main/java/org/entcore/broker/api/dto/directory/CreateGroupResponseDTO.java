

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * This class represents a response to a request to create a new group in the directory.
 * It contains the ID of the newly created group.
 * The response is used to inform the client about the result of the creation operation.
 */
public class CreateGroupResponseDTO {
  /**
   * The ID of the newly created group.
   * It is a unique identifier for the group in the directory.
   */
  private final String id;

  @JsonCreator
  public CreateGroupResponseDTO(@JsonProperty("id") String id) {
    this.id = id;
  }

  /**
   * Gets the ID of the newly created group.
   * @return The ID of the group. It is a unique identifier for the group in the directory.
   */
  public String getId() {
    return id;
  }
}
