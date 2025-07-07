package org.entcore.broker.api.dto.communication;

import java.util.Set;

public class FindVisibleProfileGroupsRequestDTOBuilder {
  private Set<String> groupIds;
  private String search;
  private Set<String> onlyGroupsWithFilters;

  public FindVisibleProfileGroupsRequestDTOBuilder setGroupIds(Set<String> groupIds) {
    this.groupIds = groupIds;
    return this;
  }

  public FindVisibleProfileGroupsRequestDTOBuilder setSearch(String search) {
    this.search = search;
    return this;
  }

  public FindVisibleProfileGroupsRequestDTOBuilder setOnlyGroupsWithFilters(Set<String> onlyGroupsWithFilters) {
    this.onlyGroupsWithFilters = onlyGroupsWithFilters;
    return this;
  }

  public FindVisibleProfileGroupsRequestDTO  build() {
    return new FindVisibleProfileGroupsRequestDTO(groupIds, search, onlyGroupsWithFilters);
  }
}