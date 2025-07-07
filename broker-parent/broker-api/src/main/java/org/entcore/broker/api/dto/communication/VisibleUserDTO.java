package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VisibleUserDTO {
  private final String id;
  private final String name;

  @JsonCreator
  public VisibleUserDTO(@JsonProperty("id") final String id,
                        @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
