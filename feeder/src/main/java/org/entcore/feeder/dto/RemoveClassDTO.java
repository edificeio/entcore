package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveClassDTO {

    private String classId;

    public RemoveClassDTO() {}

    public RemoveClassDTO(JsonObject json) {
        RemoveClassDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveClassDTOConverter.toJson(this, json);
        return json;
    }

    public String getClassId() { return classId; }
    public RemoveClassDTO setClassId(String classId) { this.classId = classId; return this; }
}
