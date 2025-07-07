package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckCommunicationExistsResponseDTO {
  private final boolean exists;

  @JsonCreator
  public CheckCommunicationExistsResponseDTO(@JsonProperty("exists") boolean exists) {
    this.exists = exists;
  }

  public boolean isExists() {
    return exists;
  }
}
