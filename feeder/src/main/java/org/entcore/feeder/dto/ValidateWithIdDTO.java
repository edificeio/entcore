package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ValidateWithIdDTO {

    private String id;
    private List<String> admlStructures;

    public ValidateWithIdDTO() {}

    public ValidateWithIdDTO(JsonObject json) {
        ValidateWithIdDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ValidateWithIdDTOConverter.toJson(this, json);
        return json;
    }

    public String getId() { return id; }
    public ValidateWithIdDTO setId(String id) { this.id = id; return this; }

    public List<String> getAdmlStructures() { return admlStructures; }
    public ValidateWithIdDTO setAdmlStructures(List<String> admlStructures) { this.admlStructures = admlStructures; return this; }
}