package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class GetUserInfoRequestDTO {
  private final Set<String> userIds;
  private final Set<String> groupIds;

  @JsonCreator
  public GetUserInfoRequestDTO(@JsonProperty("userIds") final Set<String> userIds,
                               @JsonProperty("groupIds") final Set<String> groupIds) {
    this.userIds = userIds;
    this.groupIds = groupIds;
  }

  public Set<String> getUserIds() {
    return userIds;
  }

  public Set<String> getGroupIds() {
    return groupIds;
  }
}
