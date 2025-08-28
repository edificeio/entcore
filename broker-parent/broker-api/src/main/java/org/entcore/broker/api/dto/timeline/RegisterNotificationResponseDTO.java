package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for the response to a notification registration request.
 */
public class RegisterNotificationResponseDTO {

    /**
     * The notification name that was registered (for single registrations)
     * or a status message (for batch registrations)
     */
    private final String message;

    /**
     * Constructor for RegisterNotificationResponseDTO
     *
     * @param message Either the notification name registered or a status message
     */
    @JsonCreator
    public RegisterNotificationResponseDTO(
            @JsonProperty("message") String message) {
        this.message = message;
    }

    /**
     * Gets the message (notification name or status)
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Check if this response represents a successful registration
     *
     * @return true if message doesn't contain error indicators
     */
    public boolean isSuccess() {
        return message != null &&
                !message.toLowerCase().contains("error") &&
                !message.toLowerCase().contains("fail") &&
                !message.toLowerCase().contains("invalid");
    }
}