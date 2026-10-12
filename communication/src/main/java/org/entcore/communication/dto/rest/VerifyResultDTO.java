package org.entcore.communication.dto.rest;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
public class VerifyResultDTO {

    private boolean canCommunicate;

    public VerifyResultDTO() {}

    public VerifyResultDTO(boolean canCommunicate) {
        this.canCommunicate = canCommunicate;
    }

    public VerifyResultDTO(JsonObject json) {
        this();
        VerifyResultDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        VerifyResultDTOConverter.toJson(this, json);
        return json;
    }

    public boolean isCanCommunicate() {
        return canCommunicate;
    }

    public VerifyResultDTO setCanCommunicate(boolean canCommunicate) {
        this.canCommunicate = canCommunicate;
        return this;
    }
}