package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.user.UserProfileDTOInClass;
import java.util.List;

/**
 * DTO for responding with a list of users in a class with detailed information including INE and relative information
 */
public class GetUserInClassWithParamsResponseDTO {
    private final List<UserProfileDTOInClass> data;

    @JsonCreator
    public GetUserInClassWithParamsResponseDTO(@JsonProperty("data") List<UserProfileDTOInClass> data) {
        this.data = data;
    }

    public List<UserProfileDTOInClass> getData() {
        return data;
    }
}
