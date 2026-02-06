package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.user.UserProfileDTOClassAdmin;

/**
 * DTO for responding with a list of users in a structure with basic profile information, classes informations and hobbies
 */
public class GetStructureUserResponseDTO {
    private final List<Object> data;

    @JsonCreator
    public GetStructureUserResponseDTO(@JsonProperty("data") List<Object> data) {
        this.data = data;
    }

    public List<Object> getData() {
        return data;
    }
}
