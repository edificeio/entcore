package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class FindVisibleProfileGroupsRequestDTO {
  private final Set<String> groupIds;
  private final String search;
  private final Set<String> onlyGroupsWithFilters;

  @JsonCreator
  public FindVisibleProfileGroupsRequestDTO(@JsonProperty("groupIds") final Set<String> groupIds,
                                            @JsonProperty("search") final String search,
                                            @JsonProperty("onlyGroupsWithFilters") final Set<String> onlyGroupsWithFilters) {
    this.groupIds = groupIds;
    this.search = search;
    this.onlyGroupsWithFilters = onlyGroupsWithFilters;
  }

  public Set<String> getGroupIds() {
    return groupIds;
  }

  public String getSearch() {
    return search;
  }

  public Set<String> getOnlyGroupsWithFilters() {
    return onlyGroupsWithFilters;
  }

}
