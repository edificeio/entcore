package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.user.UserProfileDTOClassAdmin;

/**
 * DTO for responding with user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetClassAdminResponseDTO {
    private final UserProfileDTOClassAdmin data;

    @JsonCreator
    public GetClassAdminResponseDTO(@JsonProperty("data") UserProfileDTOClassAdmin data) {
        this.data = data;
    }

    public UserProfileDTOClassAdmin getData() {
        return data;
    }
}
