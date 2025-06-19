package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to add communication links between a group and its users.
 * It contains a boolean indicating whether the links were successfully added.
 */
public class AddCommunicationLinksResponseDTO {
  
  /**
   * A boolean indicating whether the communication links were successfully added.
   */
  private final boolean added;

  /**
   * Creates a new response for an add communication links operation.
   *
   * @param added true if the links were successfully added, false otherwise
   */
  @JsonCreator
  public AddCommunicationLinksResponseDTO(@JsonProperty("added") boolean added) {
    this.added = added;
  }
  
  /**
   * Gets whether the communication links were successfully added.
   * @return true if the links were added, false otherwise
   */
  public boolean isAdded() {
    return added;
  }
}