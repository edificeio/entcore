package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateSubjectDTO {

    private String structureId;
    private String label;
    private String code;

    public CreateSubjectDTO() {}

    public CreateSubjectDTO(JsonObject json) {
        CreateSubjectDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateSubjectDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public CreateSubjectDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getLabel() { return label; }
    public CreateSubjectDTO setLabel(String label) { this.label = label; return this; }

    public String getCode() { return code; }
    public CreateSubjectDTO setCode(String code) { this.code = code; return this; }
}