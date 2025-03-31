package org.entcore.broker.api.appregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppRegistrationResponseDTO {
  private final boolean success;
  private final String message;

  @JsonCreator
  public AppRegistrationResponseDTO(@JsonProperty("success") final boolean success,
                                    @JsonProperty("message") final String message) {
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
