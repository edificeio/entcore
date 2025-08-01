package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;

/**
 * This class represents a request to recreate a session.
 * It can contain either a session ID or cookies from which the session ID can be extracted.
 * It also supports headers and query parameters needed for OAuth2/JWT token validation.
 */
public class SessionRecreationRequestDTO {
    /** User whose session we want to recreate.*/
    private final String userId;
    /** Current id of the session.*/
    private final String sessionId;
    /** {@code true} if we just have to refresh the session info, otherwise we will drop the previous session and
     * recreate a new one.*/
    private final boolean refreshOnly;

    @JsonCreator
    public SessionRecreationRequestDTO(@JsonProperty("userId") String userId,
                                    @JsonProperty("sessionId") String sessionId,
                                    @JsonProperty("refreshOnly") boolean refreshOnly) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.refreshOnly = refreshOnly;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isRefreshOnly() {
        return refreshOnly;
    }

    /**
     * Checks if the request has valid parameters for session recreation.
     * The request is considered valid if at least one of these conditions is met:
     * 1. A userId is provided
     * 2. A sessionId is provided
     *
     * @return true if the request contains at least one form of authentication information
     */
    @Transient()
    public boolean isValid() {
        return StringUtils.isNotBlank(userId) || StringUtils.isNotBlank(sessionId);
    }
}
