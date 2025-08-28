package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Data Transfer Object for registering multiple notification templates in the Timeline system.
 */
public class RegisterNotificationBatchRequestDTO {

    /**
     * List of notification templates to register
     */
    private final List<RegisterNotificationRequestDTO> notifications;

    @JsonCreator
    public RegisterNotificationBatchRequestDTO(
            @JsonProperty("notifications") List<RegisterNotificationRequestDTO> notifications) {
        this.notifications = notifications;
    }

    public List<RegisterNotificationRequestDTO> getNotifications() {
        return notifications;
    }

    public boolean isValid() {
        return notifications != null && !notifications.isEmpty();
    }
}