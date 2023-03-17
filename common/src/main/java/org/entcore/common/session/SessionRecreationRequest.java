package org.entcore.common.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request aut-module to recreate the session of the user.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SessionRecreationRequest {
    /** User whose session we want to recreate.*/
    private final String userId;
    /** Current id of the session.*/
    private final String sessionId;
    /** {@code true} if we just have to refresh the session info, otherwise we will drop the previous session and
     * recreate a new one.*/
    private final boolean refreshOnly;

    @JsonCreator
    public SessionRecreationRequest(@JsonProperty("userId") String userId,
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

    public String getAction() {
        return "recreate";
    }

    public boolean isRefreshOnly() {
        return refreshOnly;
    }

}
