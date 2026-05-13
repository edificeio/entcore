package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class DeleteSubjectDTO {

    private String subjectId;

    public DeleteSubjectDTO() {}

    public DeleteSubjectDTO(JsonObject json) {
        DeleteSubjectDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteSubjectDTOConverter.toJson(this, json);
        return json;
    }

    public String getSubjectId() { return subjectId; }
    public DeleteSubjectDTO setSubjectId(String subjectId) { this.subjectId = subjectId; return this; }
}