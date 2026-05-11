package org.entcore.broker.api.dto.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a specific host's authentication location.
 */
public class AuthLocationConfigDTO {

    private final String host;
    private final String loginUri;
    private final String callbackParam;

    @JsonCreator
    public AuthLocationConfigDTO(
            @JsonProperty("host") String host,
            @JsonProperty("loginUri") String loginUri,
            @JsonProperty("callbackParam") String callbackParam) {
        this.host = host;
        this.loginUri = loginUri;
        this.callbackParam = callbackParam;
    }

    public String getHost() {
        return host;
    }

    public String getLoginUri() {
        return loginUri;
    }

    public String getCallbackParam() {
        return callbackParam;
    }

    @Override
    public String toString() {
        return "AuthLocationConfigDTO{" +
                "host='" + host + '\'' +
                ", loginUri='" + loginUri + '\'' +
                ", callbackParam='" + callbackParam + '\'' +
                '}';
    }
}