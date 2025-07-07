package org.entcore.broker.api.dto.communication;

import org.entcore.broker.api.dto.OrderDTO;

import java.util.Map;
import java.util.Set;

public class FindVisibleUsersRequestDTOBuilder {
  private String userId;
  private Set<String> userIds;
  private String search;
  private Set<String> onlyUserWithProfiles;
  private boolean profile;
  private Map<String, String> fields;
  private OrderDTO order;

  public FindVisibleUsersRequestDTOBuilder setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setUserIds(Set<String> userIds) {
    this.userIds = userIds;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setSearch(String search) {
    this.search = search;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setOnlyUserWithProfiles(Set<String> onlyUserWithProfiles) {
    this.onlyUserWithProfiles = onlyUserWithProfiles;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setProfile(boolean profile) {
    this.profile = profile;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setFields(Map<String, String> fields) {
    this.fields = fields;
    return this;
  }

  public FindVisibleUsersRequestDTOBuilder setOrder(OrderDTO order) {
    this.order = order;
    return this;
  }

  public FindVisibleUsersRequestDTO build() {
    return new FindVisibleUsersRequestDTO(userId, userIds, search, onlyUserWithProfiles, profile, fields, order);
  }
}