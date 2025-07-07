package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestoreRelationshipRequestDTO {
  private final String mergedUserLogin;

  @JsonCreator
  public RestoreRelationshipRequestDTO(@JsonProperty("mergedUserLogin") final String mergedUserLogin) {
    this.mergedUserLogin = mergedUserLogin;
  }

  public String getMergedUserLogin() {
    return mergedUserLogin;
  }

}
