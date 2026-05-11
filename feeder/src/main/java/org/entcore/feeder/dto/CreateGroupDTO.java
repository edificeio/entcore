package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateGroupDTO {

    private String structureId;
    private String classId;
    private GroupDataDTO group;

    public CreateGroupDTO() {}

    public CreateGroupDTO(JsonObject json) {
        CreateGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public CreateGroupDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getClassId() { return classId; }
    public CreateGroupDTO setClassId(String classId) { this.classId = classId; return this; }

    public GroupDataDTO getGroup() { return group; }
    public CreateGroupDTO setGroup(GroupDataDTO group) { this.group = group; return this; }
}
