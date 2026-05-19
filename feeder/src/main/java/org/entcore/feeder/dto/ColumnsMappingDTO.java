package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ColumnsMappingDTO {

    private String language = "fr";
    private String path;

    public ColumnsMappingDTO() {}

    public ColumnsMappingDTO(JsonObject json) {
        ColumnsMappingDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ColumnsMappingDTOConverter.toJson(this, json);
        return json;
    }

    public String getLanguage() { return language; }
    public ColumnsMappingDTO setLanguage(String language) { this.language = language; return this; }

    public String getPath() { return path; }
    public ColumnsMappingDTO setPath(String path) { this.path = path; return this; }
}