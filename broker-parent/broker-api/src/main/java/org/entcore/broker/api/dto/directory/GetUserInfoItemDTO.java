package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetUserInfoItemDTO {
  private final String id;
  private final String displayName;

  @JsonCreator
  public GetUserInfoItemDTO(
    @JsonProperty("id") final String id,
    @JsonProperty("displayName") final String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }
}
