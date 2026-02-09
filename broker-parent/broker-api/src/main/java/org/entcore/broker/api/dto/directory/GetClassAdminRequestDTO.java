package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for requesting user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetClassAdminRequestDTO {
    private final String userId;

    @JsonCreator
    public GetClassAdminRequestDTO(@JsonProperty("userId") String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String isValid() {
        return userId != null && !userId.trim().isEmpty();
    }
}
