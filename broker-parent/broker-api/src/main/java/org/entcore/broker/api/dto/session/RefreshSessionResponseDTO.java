package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the session refresh (recreate) response.
 */
public class RefreshSessionResponseDTO {
    private final String sessionId;

    /**
     * Constructor for RefreshSessionResponseDTO.
     * @param sessionId The id of the refreshed session.
     */
    @JsonCreator
    public RefreshSessionResponseDTO(@JsonProperty("sessionId") String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @return The session id.
     */
    public String getSessionId() { return sessionId; }

    @Override
    public String toString() {
        return "RefreshSessionResponseDTO{" +
                "sessionId='" + (sessionId != null ? "***" : "null") + '\'' +
                '}';
    }
}
