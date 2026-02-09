package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for requesting user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetUserInClassWithParamsRequestDTO {
    private final String classId;
    private final boolean collectRelative;
    private final boolean ine;
    private final String type;

    @JsonCreator
    public GetUserInClassWithParamsRequestDTO(@JsonProperty("classId") String classId, @JsonProperty("collectRelative") boolean collectRelative, @JsonProperty("ine") boolean ine, @JsonProperty("type") String type) {
        this.classId = classId;
        this.collectRelative = collectRelative;
        this.ine = ine;
        this.type = type;
    }

    public String getClassId() {
        return classId;
    }

    public boolean doesCollectRelative() {
        return collectRelative;
    }

    public boolean doesCollectIne() {
        return ine;
    }

    public String getType() {
        return type;
    }

    public String isValid() {
        return classId != null && !classId.trim().isEmpty() && type != null && !type.trim().isEmpty();
    }
}