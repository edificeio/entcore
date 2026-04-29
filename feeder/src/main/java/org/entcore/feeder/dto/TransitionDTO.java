package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class TransitionDTO {

    private String structureExternalId;
    private Boolean onlyRemoveShare;

    public TransitionDTO() {}

    public TransitionDTO(JsonObject json) {
        TransitionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        TransitionDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureExternalId() { return structureExternalId; }
    public TransitionDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }

    public Boolean getOnlyRemoveShare() { return onlyRemoveShare; }
    public TransitionDTO setOnlyRemoveShare(Boolean onlyRemoveShare) { this.onlyRemoveShare = onlyRemoveShare; return this; }
}