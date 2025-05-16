package org.entcore.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseResponseDTO {
  private final boolean success;
  private final String message;

  @JsonCreator
  public BaseResponseDTO(@JsonProperty("success") boolean success,
                         @JsonProperty("message") String message) {
    this.success = success;
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }
}
