package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class StructureDetachmentDTO {

    private String structureId;
    private String parentStructureId;

    public StructureDetachmentDTO() {}

    public StructureDetachmentDTO(JsonObject json) {
        StructureDetachmentDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        StructureDetachmentDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public StructureDetachmentDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getParentStructureId() { return parentStructureId; }
    public StructureDetachmentDTO setParentStructureId(String parentStructureId) { this.parentStructureId = parentStructureId; return this; }
}