package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to recreate communication links between a group and its users.
 * It contains a boolean indicating whether the links were successfully recreated.
 */
public class RecreateCommunicationLinksResponseDTO {
  
  /**
   * A boolean indicating whether the communication links were successfully recreated.
   */
  private final boolean recreated;

  /**
   * Creates a new response for a recreate communication links operation.
   *
   * @param recreated true if the links were successfully recreated, false otherwise
   */
  @JsonCreator
  public RecreateCommunicationLinksResponseDTO(@JsonProperty("recreated") boolean recreated) {
    this.recreated = recreated;
  }
  
  /**
   * Gets whether the communication links were successfully recreated.
   * @return true if the links were recreated, false otherwise
   */
  public boolean isRecreated() {
    return recreated;
  }
}