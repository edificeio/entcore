package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RelativeStudentDTO {

    private String relativeId;
    private String studentId;

    public RelativeStudentDTO() {}

    public RelativeStudentDTO(JsonObject json) {
        RelativeStudentDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RelativeStudentDTOConverter.toJson(this, json);
        return json;
    }

    public String getRelativeId() { return relativeId; }
    public RelativeStudentDTO setRelativeId(String relativeId) { this.relativeId = relativeId; return this; }

    public String getStudentId() { return studentId; }
    public RelativeStudentDTO setStudentId(String studentId) { this.studentId = studentId; return this; }
}
