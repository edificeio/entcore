package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class DeleteUserDTO {

    private List<String> users;

    public DeleteUserDTO() {}

    public DeleteUserDTO(JsonObject json) {
        DeleteUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteUserDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getUsers() { return users; }
    public DeleteUserDTO setUsers(List<String> users) { this.users = users; return this; }
}
