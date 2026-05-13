package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveUserFunctionDTO {

    private String userId;
    private String function;

    public RemoveUserFunctionDTO() {}

    public RemoveUserFunctionDTO(JsonObject json) {
        RemoveUserFunctionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveUserFunctionDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public RemoveUserFunctionDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getFunction() { return function; }
    public RemoveUserFunctionDTO setFunction(String function) { this.function = function; return this; }
}