package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for requesting user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetStructureUsersRequestDTO {
    private final String structureId;

    @JsonCreator
    public GetStructureUsersRequestDTO(@JsonProperty("structureId") String structureId) {
        this.structureId = structureId;
    }

    public String getStructureId() {
        return structureId;
    }

    public boolean isValid() {
        return structureId != null && !structureId.trim().isEmpty();
    }
}
