package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddUserDTO {

    private String userId;
    private String structureId;
    private String classId;

    public AddUserDTO() {}

    public AddUserDTO(JsonObject json) {
        AddUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddUserDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public AddUserDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getStructureId() { return structureId; }
    public AddUserDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getClassId() { return classId; }
    public AddUserDTO setClassId(String classId) { this.classId = classId; return this; }
}
