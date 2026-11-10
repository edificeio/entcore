package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RestoreUserDTO {

    private List<String> users;

    public RestoreUserDTO() {}

    public RestoreUserDTO(JsonObject json) {
        RestoreUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RestoreUserDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getUsers() { return users; }
    public RestoreUserDTO setUsers(List<String> users) { this.users = users; return this; }
}