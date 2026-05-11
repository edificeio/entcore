package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateClassDTO {

    private String classId;
    private String name;
    private String level;

    public UpdateClassDTO() {}

    public UpdateClassDTO(JsonObject json) {
        UpdateClassDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateClassDTOConverter.toJson(this, json);
        return json;
    }

    public String getClassId() { return classId; }
    public UpdateClassDTO setClassId(String classId) { this.classId = classId; return this; }

    public String getName() { return name; }
    public UpdateClassDTO setName(String name) { this.name = name; return this; }

    public String getLevel() { return level; }
    public UpdateClassDTO setLevel(String level) { this.level = level; return this; }
}