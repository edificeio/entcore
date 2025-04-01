

package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateSharesResponseDTO {
  private final String userId;

  @JsonCreator
  public CreateSharesResponseDTO(@JsonProperty("userId") String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }
}
