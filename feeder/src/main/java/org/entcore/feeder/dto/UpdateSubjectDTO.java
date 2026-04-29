package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateSubjectDTO {

    private String id;
    private String label;
    private String code;

    public UpdateSubjectDTO() {}

    public UpdateSubjectDTO(JsonObject json) {
        UpdateSubjectDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateSubjectDTOConverter.toJson(this, json);
        return json;
    }

    public String getId() { return id; }
    public UpdateSubjectDTO setId(String id) { this.id = id; return this; }

    public String getLabel() { return label; }
    public UpdateSubjectDTO setLabel(String label) { this.label = label; return this; }

    public String getCode() { return code; }
    public UpdateSubjectDTO setCode(String code) { this.code = code; return this; }
}