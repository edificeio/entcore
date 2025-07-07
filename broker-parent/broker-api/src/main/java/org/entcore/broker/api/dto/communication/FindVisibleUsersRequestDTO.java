package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.OrderDTO;

import java.util.Map;
import java.util.Set;

public class FindVisibleUsersRequestDTO {
  private final String userId;
  private final Set<String> userIds;
  private final String search;
  private final Set<String> onlyUserWithProfiles;
  private final boolean profile;
  private final Map<String, String> fields;
  private final OrderDTO order;

  @JsonCreator
  public FindVisibleUsersRequestDTO(@JsonProperty("userId") final String userId,
                                    @JsonProperty("userIds") final Set<String> userIds,
                                    @JsonProperty("search") final String search,
                                    @JsonProperty("onlyUserWithProfiles") final Set<String> onlyUserWithProfiles,
                                    @JsonProperty("profile") final boolean profile,
                                    @JsonProperty("fields") final Map<String, String> fields,
                                    @JsonProperty("order") final OrderDTO order) {
    this.userId = userId;
    this.userIds = userIds;
    this.search = search;
    this.onlyUserWithProfiles = onlyUserWithProfiles;
    this.profile = profile;
    this.fields = fields;
    this.order = order;
  }

  public String getUserId() {
    return userId;
  }

  public Set<String> getUserIds() {
    return userIds;
  }

  public String getSearch() {
    return search;
  }

  public Set<String> getOnlyUserWithProfiles() {
    return onlyUserWithProfiles;
  }

  public boolean isProfile() {
    return profile;
  }

  public Map<String, String> getFields() {
    return fields;
  }

  public OrderDTO getOrder() {
    return order;
  }
}
