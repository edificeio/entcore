package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to create a communication link between two groups.
 * It contains a boolean indicating whether the link was successfully created.
 */
public class AddLinkBetweenGroupsResponseDTO {
  
  /**
   * A boolean indicating whether the communication link was successfully created.
   */
  private final boolean created;

  /**
   * Creates a new response for an add link between groups operation.
   *
   * @param created true if the link was successfully created, false otherwise
   */
  @JsonCreator
  public AddLinkBetweenGroupsResponseDTO(@JsonProperty("created") boolean created) {
    this.created = created;
  }
  
  /**
   * Gets whether the communication link was successfully created.
   * @return true if the link was created, false otherwise
   */
  public boolean isCreated() {
    return created;
  }
}