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

    @JsonCreator
    public SendNotificationResponseDTO(
            @JsonProperty("recipientCount") int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public int getRecipientCount() {
        return recipientCount;
    }
}