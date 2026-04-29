package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ImportDTO {

    private String feeder;
    private String structureExternalId;
    private Boolean preDelete;
    private Boolean transition;

    public ImportDTO() {}

    public ImportDTO(JsonObject json) {
        ImportDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ImportDTOConverter.toJson(this, json);
        return json;
    }

    public String getFeeder() { return feeder; }
    public ImportDTO setFeeder(String feeder) { this.feeder = feeder; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public ImportDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }

    public Boolean getPreDelete() { return preDelete; }
    public ImportDTO setPreDelete(Boolean preDelete) { this.preDelete = preDelete; return this; }

    public Boolean getTransition() { return transition; }
    public ImportDTO setTransition(Boolean transition) { this.transition = transition; return this; }
}