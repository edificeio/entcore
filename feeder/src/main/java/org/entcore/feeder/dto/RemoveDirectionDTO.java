package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveDirectionDTO {

    private String userId;
    private String structureExternalId;

    public RemoveDirectionDTO() {}

    public RemoveDirectionDTO(JsonObject json) {
        RemoveDirectionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveDirectionDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public RemoveDirectionDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public RemoveDirectionDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }
}