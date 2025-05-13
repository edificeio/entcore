package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * This class represents a request to find a session.
 * It can contain either a session ID or cookies from which the session ID can be extracted.
 * It also supports headers and query parameters needed for OAuth2/JWT token validation.
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
   * HTTP headers from the original request.
   * These can be used to extract authentication tokens like OAuth2 bearer tokens
   * from Authorization header.
   * May be empty if not provided.
   */
  private final Map<String, String> headers;

  /**
   * Query parameters from the original request URL.
   * These can contain JWT related parameters
   * May be empty if not provided.
   */
  private final Map<String, String> params;
  
  /**
   * The path prefix from the original request.
   * Used for OAuth2 scope verification to determine what application
   * or resource the token is trying to access.
   * Can be null if scope checking is not needed.
   */
  private final String pathPrefix;
  
  /**
   * The complete path from the original request URL.
   * This contains the full path component of the URL without query parameters.
   * Can be used for more precise authorization decisions or logging.
   */
  private final String path;

  /**
   * Creates a new instance of FindSessionRequestDTO.
   *
   * @param sessionId The ID of the session to be found. Can be null if cookie is provided.
   * @param cookies The Cookie header content that may contain the signed session ID. Can be null if sessionId is provided.
   * @param headers HTTP headers that may contain OAuth tokens (e.g., Authorization header). Can be null.
   * @param params Query parameters that may contain JWT parameters. Can be null.
   * @param pathPrefix Path prefix used for OAuth scope verification. Can be null if scope check not needed.
   * @param path The complete path from the original request URL. Can be null.
   */
  @JsonCreator
  public FindSessionRequestDTO(
          @JsonProperty("sessionId") String sessionId,
          @JsonProperty("cookies") String cookies,
          @JsonProperty("headers") Map<String, String> headers,
          @JsonProperty("params") Map<String, String> params,
          @JsonProperty("pathPrefix") String pathPrefix,
          @JsonProperty("path") String path) {
    this.sessionId = sessionId;
    this.cookies = cookies;
    this.headers = headers != null ? headers : Collections.emptyMap();
    this.params = params != null ? params : Collections.emptyMap();
    this.pathPrefix = pathPrefix;
    this.path = path;
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
   * Gets the HTTP headers from the original request.
   * These can contain authentication tokens like OAuth2 bearer tokens.
   *
   * @return Map of HTTP header names to values. Never null, but may be empty.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Gets the query parameters from the original request.
   * These can include JWT related parameters.
   *
   * @return Map of parameter names to values. Never null, but may be empty.
   */
  public Map<String, String> getParams() {
    return params;
  }
  
  /**
   * Gets the path prefix for OAuth scope verification.
   * This is used to determine which application/resource the OAuth token
   * is trying to access and verify corresponding scopes.
   *
   * @return Path prefix string or null if not specified
   */
  public String getPathPrefix() {
    return pathPrefix;
  }
  
  /**
   * Gets the complete path from the original request URL.
   * This can be used for more granular authorization decisions based on
   * the specific endpoint being accessed.
   *
   * @return The complete path string or null if not specified
   */
  public String getPath() {
    return path;
  }

  /**
   * Checks if the request has valid parameters for session identification.
   * The request is considered valid if at least one of these conditions is met:
   * 1. A sessionId is provided
   * 2. Cookies containing session information are provided
   * 3. HTTP headers are provided (could contain JWT/OAuth2/Bearer tokens)
   * 4. Query parameters are provided (could contain JWT tokens)
   *
   * @return true if the request contains at least one form of authentication information
   */
  public boolean isValid() {
    return StringUtils.isNotBlank(sessionId) || 
           StringUtils.isNotBlank(cookies) || 
           !headers.isEmpty() || 
           !params.isEmpty();
  }

  @Override
  public String toString() {
    return "FindSessionRequestDTO{" +
            "sessionId='" + (sessionId != null ? "***" : "null") + '\'' +
            ", cookieProvided=" + (StringUtils.isNotBlank(cookies)) +
            ", headersCount=" + headers.size() +
            ", paramsCount=" + params.size() +
            ", pathPrefix='" + pathPrefix + '\'' +
            ", path='" + path + '\'' +
            '}';
  }
}