package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.user.UserProfileDTOStructure;
import java.util.List;

/**
 * DTO for responding with a list of users in a structure with basic profile information, classes informations and hobbies
 */
public class GetStructureUsersResponseDTO {
    private final List<UserProfileDTOStructure> data;

    @JsonCreator
    public GetStructureUsersResponseDTO(@JsonProperty("data") List<UserProfileDTOStructure> data) {
        this.data = data;
    }

    public List<UserProfileDTOStructure> getData() {
        return data;
    }
}
