package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class GetGroupsResponseDTO {
  private final Set<GroupDTO> groups;

  @JsonCreator
  public GetGroupsResponseDTO(@JsonProperty("groups") final Set<GroupDTO> groups) {
    this.groups = groups;
  }

  public Set<GroupDTO> getGroups() {
    return groups;
  }
}
