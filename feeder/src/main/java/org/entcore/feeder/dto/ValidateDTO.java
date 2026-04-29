package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ValidateDTO {

    private String feeder;
    private String language;
    private String structureExternalId;
    private Boolean preDelete;
    private String path;
    private List<String> admlStructures;

    public ValidateDTO() {}

    public ValidateDTO(JsonObject json) {
        ValidateDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ValidateDTOConverter.toJson(this, json);
        return json;
    }

    public String getFeeder() { return feeder; }
    public ValidateDTO setFeeder(String feeder) { this.feeder = feeder; return this; }

    public String getLanguage() { return language; }
    public ValidateDTO setLanguage(String language) { this.language = language; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public ValidateDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }

    public Boolean getPreDelete() { return preDelete; }
    public ValidateDTO setPreDelete(Boolean preDelete) { this.preDelete = preDelete; return this; }

    public String getPath() { return path; }
    public ValidateDTO setPath(String path) { this.path = path; return this; }

    public List<String> getAdmlStructures() { return admlStructures; }
    public ValidateDTO setAdmlStructures(List<String> admlStructures) { this.admlStructures = admlStructures; return this; }
}