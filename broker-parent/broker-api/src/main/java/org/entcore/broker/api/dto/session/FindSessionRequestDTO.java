package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a request to find a session.
 * It can contain either a session ID or cookies from which the session ID can be extracted.
 */
public class FindSessionRequestDTO {
  /**
   * The ID of the session to be found.
   * This is a unique identifier for the session in the system.
   * It can be null if cookies are provided instead.
   */
  private final String sessionId;

  /**
   * The Cookie header content that may contain the signed session ID.
   * This can be used when a direct session ID is not available.
   * It can be null if sessionId is provided directly.
   */
  private final String cookies;

  /**
   * Creates a new instance of FindSessionRequestDTO.
   *
   * @param sessionId The ID of the session to be found. Can be null if cookie is provided.
   * @param cookies The Cookie header content that may contain the signed session ID. Can be null if sessionId is provided.
   */
  @JsonCreator
  public FindSessionRequestDTO(
          @JsonProperty("sessionId") String sessionId,
          @JsonProperty("cookies") String cookies) {
    this.sessionId = sessionId;
    this.cookies = cookies;
  }

  /**
   * Gets the ID of the session.
   *
   * @return The unique identifier of the session in the system. Can be null if cookies are used instead.
   */
  public String getSessionId() { 
    return sessionId; 
  }

  /**
   * Gets the Cookie header content.
   *
   * @return The Cookie header content that may contain the session ID. Can be null if sessionId is used directly.
   */
  public String getCookies() { 
    return cookies; 
  }

  /**
   * Checks if the request has valid parameters for session identification.
   * At least one of sessionId or cookie must be provided.
   *
   * @return true if either sessionId or cookie is provided, false if both are missing or empty.
   */
  public boolean isValid() {
    return StringUtils.isNotBlank(sessionId) || StringUtils.isNotBlank(cookies);
  }

  @Override
  public String toString() {
    return "FindSessionRequestDTO{" +
            "sessionId='" + (sessionId != null ? "***" : "null") + '\'' +
            ", cookieProvided=" + (StringUtils.isNotBlank(cookies)) +
            '}';
  }
}