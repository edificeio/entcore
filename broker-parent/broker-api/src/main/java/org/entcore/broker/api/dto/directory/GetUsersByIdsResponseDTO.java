package org.entcore.broker.api.dto.directory;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for responding with user information for multiple user IDs.
 */
public class GetUsersByIdsResponseDTO {
    private final List<UserDTO> users;

    @JsonCreator
    public GetUsersByIdsResponseDTO(@JsonProperty("users") List<UserDTO> users) {
        this.users = users;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return "GetUsersByIdsResponseDTO{" +
                "users=" + users +
                '}';
    }
}