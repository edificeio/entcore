package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddHeadTeacherDTO {

    private String userId;
    private String classExternalId;
    private String structureExternalId;

    public AddHeadTeacherDTO() {}

    public AddHeadTeacherDTO(JsonObject json) {
        AddHeadTeacherDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddHeadTeacherDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public AddHeadTeacherDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getClassExternalId() { return classExternalId; }
    public AddHeadTeacherDTO setClassExternalId(String classExternalId) { this.classExternalId = classExternalId; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public AddHeadTeacherDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }
}