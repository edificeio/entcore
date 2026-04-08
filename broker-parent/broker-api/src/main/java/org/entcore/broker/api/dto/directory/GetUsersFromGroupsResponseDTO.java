package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GetUsersFromGroupsResponseDTO {
  private final List<UserDTO> users;

  @JsonCreator
  public GetUsersFromGroupsResponseDTO(@JsonProperty("users") List<UserDTO> users) {
    this.users = users;
  }

  public List<UserDTO> getUsers() {
    return users;
  }

  @Override
  public String toString() {
    return "GetUsersFromGroupsResponseDTO{" +
        "users=" + users +
        '}';
  }
}
