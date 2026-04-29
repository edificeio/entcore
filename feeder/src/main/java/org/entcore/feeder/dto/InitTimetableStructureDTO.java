package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class InitTimetableStructureDTO {

    private String type;
    private String structureId;

    public InitTimetableStructureDTO() {}

    public InitTimetableStructureDTO(JsonObject json) {
        InitTimetableStructureDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        InitTimetableStructureDTOConverter.toJson(this, json);
        return json;
    }

    public String getType() { return type; }
    public InitTimetableStructureDTO setType(String type) { this.type = type; return this; }

    public String getStructureId() { return structureId; }
    public InitTimetableStructureDTO setStructureId(String structureId) { this.structureId = structureId; return this; }
}