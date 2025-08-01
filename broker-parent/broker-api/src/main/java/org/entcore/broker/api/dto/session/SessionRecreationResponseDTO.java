package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response containing session information.
 * It includes the session details encapsulated in a SessionDto object.
 * The session object contains information about the session such as its ID, name, and associated actions.
 */
public class SessionRecreationResponseDTO {
    /**
     * The session object.
     * This contains the details of the recreated session.
     */
    private final SessionDto session;
    @JsonCreator
    public SessionRecreationResponseDTO(@JsonProperty("session") SessionDto session) {
        this.session = session;
    }

    /**
     * Gets the session object.
     *
     * @return The session object containing details of the session.
     */
    public SessionDto getSession() { return session; }
}
