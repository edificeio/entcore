package org.entcore.broker.api.dto.directory;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for responding with user display names mapped to their ENT IDs.
 */
public class GetUserDisplayNamesResponseDTO {
    private final Map<String, String> userDisplayNames;

    @JsonCreator
    public GetUserDisplayNamesResponseDTO(@JsonProperty("userDisplayNames") Map<String, String> userDisplayNames) {
        this.userDisplayNames = userDisplayNames;
    }

    public Map<String, String> getUserDisplayNames() {
        return userDisplayNames;
    }

    public String toString() {
        return "GetUserDisplayNamesResponseDTO{" +
                "userDisplayNames=" + userDisplayNames +
                '}';
    }
}