package org.entcore.broker.api.dto.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FindVisibleUsersResponseDTO {
  private final List<VisibleUserDTO> users;

  @JsonCreator
  public FindVisibleUsersResponseDTO(@JsonProperty("groups") List<VisibleUserDTO> users) {
    this.users = users;
  }

  public List<VisibleUserDTO> getUsers() {
    return users;
  }
}
