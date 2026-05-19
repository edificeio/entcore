package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ImportWithIdDTO {

    private String id;

    public ImportWithIdDTO() {}

    public ImportWithIdDTO(JsonObject json) {
        ImportWithIdDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ImportWithIdDTOConverter.toJson(this, json);
        return json;
    }

    public String getId() { return id; }
    public ImportWithIdDTO setId(String id) { this.id = id; return this; }
}