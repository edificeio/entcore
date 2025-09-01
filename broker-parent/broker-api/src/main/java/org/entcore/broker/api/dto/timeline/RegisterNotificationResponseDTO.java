package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for the response to a notification registration request.
 */
public class RegisterNotificationResponseDTO {

    /**
     * The number of notifications registered
     */
    private final int count;

    /**
     * Constructor for RegisterNotificationResponseDTO
     *
     * @param count Number of notifications registered
     */
    @JsonCreator
    public RegisterNotificationResponseDTO(
            @JsonProperty("count") int count) {
        this.count = count;
    }

    /**
     * Gets the number of notifications registered
     *
     * @return The count
     */
    public int getCount() {
        return count;
    }
}