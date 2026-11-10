package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateUserLoginDTO {

    private String userId;
    private String login;

    public UpdateUserLoginDTO() {}

    public UpdateUserLoginDTO(JsonObject json) {
        UpdateUserLoginDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateUserLoginDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public UpdateUserLoginDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getLogin() { return login; }
    public UpdateUserLoginDTO setLogin(String login) { this.login = login; return this; }
}
