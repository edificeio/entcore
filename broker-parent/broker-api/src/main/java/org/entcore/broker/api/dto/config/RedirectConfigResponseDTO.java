package org.entcore.broker.api.dto.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for redirect configuration.
 * Contains the login URI, callback parameter, and per-host auth locations.
 */
public class RedirectConfigResponseDTO {

    private final String loginUri;
    private final String callbackParam;
    private final List<AuthLocationConfigDTO> authLocations;
    private final List<String> allowedHosts;

    @JsonCreator
    public RedirectConfigResponseDTO(
            @JsonProperty("loginUri") String loginUri,
            @JsonProperty("callbackParam") String callbackParam,
            @JsonProperty("authLocations") List<AuthLocationConfigDTO> authLocations,
            @JsonProperty("allowedHosts") List<String> allowedHosts) {
        this.loginUri = loginUri;
        this.callbackParam = callbackParam;
        this.authLocations = authLocations;
        this.allowedHosts = allowedHosts;
    }

    public String getLoginUri() {
        return loginUri;
    }

    public String getCallbackParam() {
        return callbackParam;
    }

    public List<AuthLocationConfigDTO> getAuthLocations() {
        return authLocations;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    @Override
    public String toString() {
        return "RedirectConfigResponseDTO{" +
                "loginUri='" + loginUri + '\'' +
                ", callbackParam='" + callbackParam + '\'' +
                ", authLocations=" + authLocations +
                ", allowedHosts=" + allowedHosts +
                '}';
    }
}