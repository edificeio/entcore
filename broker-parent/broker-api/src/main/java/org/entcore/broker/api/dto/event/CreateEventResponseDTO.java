// filepath: /Users/nabil/workspace/entcore/broker-parent/broker-api/src/main/java/org/entcore/broker/api/dto/event/CreateEventResponseDTO.java
package org.entcore.broker.api.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for event creation responses.
 * Contains the ID of the created event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEventResponseDTO {
    private final String eventId;

    /**
     * Constructor with event ID
     * @param eventId The ID of the created event
     */
    @JsonCreator
    public CreateEventResponseDTO(@JsonProperty("eventId") String eventId) {
        this.eventId = eventId;
    }

    /**
     * Get the event ID
     * @return The ID of the created event
     */
    @JsonProperty("eventId")
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String toString() {
        return "CreateEventResponseDTO{eventId='" + (eventId != null ? eventId : "null") + "'}";
    }
}