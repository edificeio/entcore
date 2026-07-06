package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveUserDTO {

    private String userId;
    private String structureId;
    private String classId;

    public RemoveUserDTO() {}

    public RemoveUserDTO(JsonObject json) {
        RemoveUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveUserDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public RemoveUserDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getStructureId() { return structureId; }
    public RemoveUserDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getClassId() { return classId; }
    public RemoveUserDTO setClassId(String classId) { this.classId = classId; return this; }
}
