package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FindVisibleProfileGroupsResponseDTO {
  private final List<VisibleGroupDTO> groups;

  @JsonCreator
  public FindVisibleProfileGroupsResponseDTO(@JsonProperty("groups") List<VisibleGroupDTO> groups) {
    this.groups = groups;
  }

  public List<VisibleGroupDTO> getGroups() {
    return groups;
  }
}
