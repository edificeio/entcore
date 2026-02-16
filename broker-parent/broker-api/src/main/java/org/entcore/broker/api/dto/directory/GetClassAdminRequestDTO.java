package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * DTO for requesting user by its ENT ID with basic profile information, classes informations and hobbies
 */
public class GetClassAdminRequestDTO {
    private final String userId;
    /** Extra fields to include in the response. */
    private final Set<ClassIncludeField> includes;

    @JsonCreator
    public GetClassAdminRequestDTO(@JsonProperty("userId") String userId,
                                   @JsonProperty("includes") Set<ClassIncludeField> includes) {
        this.userId = userId;
        this.includes = includes;
    }

    public String getUserId() {
        return userId;
    }

    @Transient
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty();
    }

    public Set<ClassIncludeField> getIncludes() {
        return includes;
    }
}
