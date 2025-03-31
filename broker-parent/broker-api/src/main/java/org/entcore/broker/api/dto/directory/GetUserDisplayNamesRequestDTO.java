package org.entcore.broker.api.dto.directory;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.Transient;
/**
 * DTO for requesting user display names by their ENT IDs.
 */
public class GetUserDisplayNamesRequestDTO {
    private final List<String> userIds;

    @JsonCreator
    public GetUserDisplayNamesRequestDTO(@JsonProperty("userIds") List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    /**
     * Validates that the request contains the necessary data.
     * @return true if the request is valid, false otherwise
     */
    @Transient()
    public boolean isValid() {
        return userIds != null && !userIds.isEmpty();
    }

    public String toString() {
        return "GetUserDisplayNamesRequestDTO{" +
                "userIds=" + userIds +
                '}';
    }
}