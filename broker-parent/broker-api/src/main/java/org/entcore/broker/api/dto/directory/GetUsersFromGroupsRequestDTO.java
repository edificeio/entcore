package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.Transient;
import java.util.List;

public class GetUsersFromGroupsRequestDTO {
  private final List<String> groupIds;

  @JsonCreator
  public GetUsersFromGroupsRequestDTO(@JsonProperty("groupIds") List<String> groupIds) {
    this.groupIds = groupIds;
  }

  public List<String> getGroupIds() {
    return groupIds;
  }

  @Transient()
  public boolean isValid() {
    return groupIds != null && !groupIds.isEmpty();
  }

  @Override
  public String toString() {
    return "GetUsersFromGroupsRequestDTO{" +
        "groupIds=" + groupIds +
        '}';
  }
}
