package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveUsersDTO {

    private List<String> userIds;
    private String structureId;
    private List<String> classIds;

    public RemoveUsersDTO() {}

    public RemoveUsersDTO(JsonObject json) {
        RemoveUsersDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveUsersDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getUserIds() { return userIds; }
    public RemoveUsersDTO setUserIds(List<String> userIds) { this.userIds = userIds; return this; }

    public String getStructureId() { return structureId; }
    public RemoveUsersDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public List<String> getClassIds() { return classIds; }
    public RemoveUsersDTO setClassIds(List<String> classIds) { this.classIds = classIds; return this; }
}
