package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to remove communication links between a group and its users.
 * It contains a boolean indicating whether the links were successfully removed.
 */
public class RemoveCommunicationLinksResponseDTO {
  
  /**
   * A boolean indicating whether the communication links were successfully removed.
   */
  private final boolean removed;

  /**
   * Creates a new response for a remove communication links operation.
   *
   * @param removed true if the links were successfully removed, false otherwise
   */
  @JsonCreator
  public RemoveCommunicationLinksResponseDTO(@JsonProperty("removed") boolean removed) {
    this.removed = removed;
  }
  
  /**
   * Gets whether the communication links were successfully removed.
   * @return true if the links were removed, false otherwise
   */
  public boolean isRemoved() {
    return removed;
  }
}