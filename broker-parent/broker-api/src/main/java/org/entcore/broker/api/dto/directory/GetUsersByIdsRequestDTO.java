package org.entcore.broker.api.dto.directory;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.Transient;

/**
 * DTO for requesting user information by their ENT IDs.
 */
public class GetUsersByIdsRequestDTO {
    private final List<String> userIds;

    @JsonCreator
    public GetUsersByIdsRequestDTO(@JsonProperty("userIds") List<String> userIds) {
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

    @Override
    public String toString() {
        return "GetUsersByIdsRequestDTO{" +
                "userIds=" + userIds +
                '}';
    }
}