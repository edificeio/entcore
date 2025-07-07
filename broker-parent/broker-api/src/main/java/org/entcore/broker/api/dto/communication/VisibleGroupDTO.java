package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VisibleGroupDTO {
  private final String id;
  private final String name;
  private final String groupDisplayName;

  @JsonCreator
  public VisibleGroupDTO(@JsonProperty("id") final String id,
                         @JsonProperty("name") String name,
                         @JsonProperty("groupDisplayName") String groupDisplayName) {
    this.id = id;
    this.name = name;
    this.groupDisplayName = groupDisplayName;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getGroupDisplayName() {
    return groupDisplayName;
  }
}
