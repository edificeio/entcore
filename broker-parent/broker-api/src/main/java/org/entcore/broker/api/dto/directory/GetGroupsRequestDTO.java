package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class GetGroupsRequestDTO {
  private final Set<String> groupIds;

  @JsonCreator
  public GetGroupsRequestDTO(@JsonProperty("groupIds") final Set<String> groupIds) {
    this.groupIds = groupIds;
  }

  public Set<String> getGroupIds() {
    return groupIds;
  }
}
