package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UnlinkRelativeStudentDTO {

    private String relativeId;
    private String studentId;

    public UnlinkRelativeStudentDTO() {}

    public UnlinkRelativeStudentDTO(JsonObject json) {
        UnlinkRelativeStudentDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UnlinkRelativeStudentDTOConverter.toJson(this, json);
        return json;
    }

    public String getRelativeId() { return relativeId; }
    public UnlinkRelativeStudentDTO setRelativeId(String relativeId) { this.relativeId = relativeId; return this; }

    public String getStudentId() { return studentId; }
    public UnlinkRelativeStudentDTO setStudentId(String studentId) { this.studentId = studentId; return this; }
}
