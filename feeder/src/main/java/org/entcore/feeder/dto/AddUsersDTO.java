package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddUsersDTO {

    private List<String> userIds;
    private String structureId;
    private String classId;

    public AddUsersDTO() {}

    public AddUsersDTO(JsonObject json) {
        AddUsersDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddUsersDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getUserIds() { return userIds; }
    public AddUsersDTO setUserIds(List<String> userIds) { this.userIds = userIds; return this; }

    public String getStructureId() { return structureId; }
    public AddUsersDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getClassId() { return classId; }
    public AddUsersDTO setClassId(String classId) { this.classId = classId; return this; }
}
