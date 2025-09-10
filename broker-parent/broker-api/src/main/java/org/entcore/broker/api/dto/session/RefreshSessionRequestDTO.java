package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the session refresh (recreate) request.
 */
public class RefreshSessionRequestDTO {
    private final String userId;
    private final String sessionId;
    private final boolean refreshOnly;

    /**
     * Constructor for RefreshSessionRequestDTO.
     * @param userId The user whose session should be refreshed.
     * @param sessionId The current session id.
     * @param refreshOnly If true, only refresh session info, otherwise drop and recreate.
     */
    @JsonCreator
    public RefreshSessionRequestDTO(
            @JsonProperty("userId") String userId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("refreshOnly") boolean refreshOnly) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.refreshOnly = refreshOnly;
    }

    /**
     * @return The user id.
     */
    public String getUserId() { return userId; }
    /**
     * @return The session id.
     */
    public String getSessionId() { return sessionId; }
    /**
     * @return True if only refresh, false to drop and recreate.
     */
    public boolean isRefreshOnly() { return refreshOnly; }
    /**
     * Checks if the request is valid (userId and sessionId must not be empty).
     * @return true if valid, false otherwise.
     */
    public boolean isValid() {
        return userId != null && !userId.isEmpty() && sessionId != null && !sessionId.isEmpty();
    }

    @Override
    public String toString() {
        return "RefreshSessionRequestDTO{" +
                "userId='" + (userId != null ? userId : "null") + '\'' +
                ", sessionId='" + (sessionId != null ? "***" : "null") + '\'' +
                ", refreshOnly=" + refreshOnly +
                '}';
    }
}
