package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddDirectionDTO {

    private String userId;
    private String structureExternalId;

    public AddDirectionDTO() {}

    public AddDirectionDTO(JsonObject json) {
        AddDirectionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddDirectionDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public AddDirectionDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getStructureExternalId() { return structureExternalId; }
    public AddDirectionDTO setStructureExternalId(String structureExternalId) { this.structureExternalId = structureExternalId; return this; }
}