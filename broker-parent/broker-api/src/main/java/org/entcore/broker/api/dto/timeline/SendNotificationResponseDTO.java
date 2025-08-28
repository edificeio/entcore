package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for the response to a notification send request.
 */
public class SendNotificationResponseDTO {

    /**
     * Number of recipients the notification was sent to
     */
    private final int recipientCount;

    /**
     * ID of the notification or error message if sending failed
     */
    private final String message;

    @JsonCreator
    public SendNotificationResponseDTO(
            @JsonProperty("recipientCount") int recipientCount,
            @JsonProperty("message") String message) {

        this.recipientCount = recipientCount;
        this.message = message;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Check if this is a successful response
     *
     * @return true if recipients > 0 or message is not an error message
     */
    public boolean isSuccess() {
        return recipientCount > 0 ||
                (message != null &&
                        !message.toLowerCase().contains("error") &&
                        !message.toLowerCase().contains("fail"));
    }

    /**
     * Gets the notification ID if successful
     *
     * @return The notification ID or null if this is an error response
     */
    public String getNotificationId() {
        return isSuccess() ? message : null;
    }

    /**
     * Gets the error message if failed
     *
     * @return The error message or null if this is a success response
     */
    public String getError() {
        return isSuccess() ? null : message;
    }
}