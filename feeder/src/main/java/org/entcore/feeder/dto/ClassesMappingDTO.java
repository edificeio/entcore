package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ClassesMappingDTO {

    private String language;
    private String path;

    public ClassesMappingDTO() {}

    public ClassesMappingDTO(JsonObject json) {
        ClassesMappingDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ClassesMappingDTOConverter.toJson(this, json);
        return json;
    }

    public String getLanguage() { return language; }
    public ClassesMappingDTO setLanguage(String language) { this.language = language; return this; }

    public String getPath() { return path; }
    public ClassesMappingDTO setPath(String path) { this.path = path; return this; }
}