

package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import java.util.List;

/**
 * This class represents a response containing session information.
 * It includes the session details encapsulated in a SessionDto object.
 * The session object contains information about the session such as its ID, name, and associated actions.
 */
public class FindSessionResponseDTO {
  /**
   * The session object.
   * This contains the details of the session being requested.
   */
  private final SessionDto session;

  /**
   * Cookies to be set on the client.
   * These are returned when a session is created via query param token authentication.
   */
  private final List<String> cookies;

  @JsonCreator
  public FindSessionResponseDTO(@JsonProperty("session") SessionDto session,
                                @JsonProperty("cookies") List<String> cookies) {
    this.session = session;
    this.cookies = cookies;
  }

  /**
   * Constructor for backward compatibility when no cookies are provided.
   * @param session the session object
   */
  public FindSessionResponseDTO(SessionDto session) {
    this.session = session;
    this.cookies = null;
  }

  /**
   * Gets the session object.
   *
   * @return The session object containing details of the session.
   */
  public SessionDto getSession() { return session; }

  /**
   * Gets the cookies to be set on the client.
   *
   * @return The list of cookie header values, or null if no cookies were set.
   */
  public List<String> getCookies() { return cookies; }
}
