package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.user.UserProfileDTO;

/**
 * DTO for responding with user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetClassAdminResponseDTO {
    private final UserProfileDTO data;

    @JsonCreator
    public GetClassAdminResponseDTO(@JsonProperty("data") UserProfileDTO data) {
        this.data = data;
    }

    public UserProfileDTO getData() {
        return data;
    }
}
