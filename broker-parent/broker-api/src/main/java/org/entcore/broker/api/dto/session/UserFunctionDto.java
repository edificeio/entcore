package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO to represent a function assigned to a user.
 */
public class UserFunctionDto {
    private final List<String> scope;
    private final String code;

    @JsonCreator
    public UserFunctionDto(
            @JsonProperty("scope") List<String> scope,
            @JsonProperty("code") String code) {
        this.scope = scope;
        this.code = code;
    }

    public List<String> getScope() {
        return scope;
    }

    public String getCode() {
        return code;
    }
}
