package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateHeadTeacherDTO {

    private String userId;
    private String classExternalId;
    private String structureExternalId;

    public UpdateHeadTeacherDTO() {}

    public UpdateHeadTeacherDTO(JsonObject json) {
        UpdateHeadTeacherDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateHeadTeacherDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public UpdateHeadTeacherDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getClassExternalId() { return classExternalId; }
    public UpdateHeadTeacherDTO setClassExternalId(String classExternalId) { this.classExternalId = classExternalId; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public UpdateHeadTeacherDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }
}