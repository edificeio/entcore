package org.entcore.broker.api.dto.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for config.redirect broker listener.
 * Currently empty as the endpoint doesn't require any parameters.
 */
public class GetRedirectConfigRequestDTO {

    @JsonCreator
    public GetRedirectConfigRequestDTO() {
    }
}