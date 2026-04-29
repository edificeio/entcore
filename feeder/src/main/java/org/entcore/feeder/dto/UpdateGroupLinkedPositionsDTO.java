package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateGroupLinkedPositionsDTO {

    private String userPosition;

    public UpdateGroupLinkedPositionsDTO() {}

    public UpdateGroupLinkedPositionsDTO(JsonObject json) {
        UpdateGroupLinkedPositionsDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateGroupLinkedPositionsDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserPosition() { return userPosition; }
    public UpdateGroupLinkedPositionsDTO setUserPosition(String userPosition) { this.userPosition = userPosition; return this; }
}