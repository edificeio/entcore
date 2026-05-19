package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateUsersOldPlatformDTO {

    private JsonObject criteria;
    private JsonObject update;

    public UpdateUsersOldPlatformDTO() {}

    public UpdateUsersOldPlatformDTO(JsonObject json) {
        UpdateUsersOldPlatformDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateUsersOldPlatformDTOConverter.toJson(this, json);
        return json;
    }

    public JsonObject getCriteria() { return criteria; }
    public UpdateUsersOldPlatformDTO setCriteria(JsonObject criteria) { this.criteria = criteria; return this; }

    public JsonObject getUpdate() { return update; }
    public UpdateUsersOldPlatformDTO setUpdate(JsonObject update) { this.update = update; return this; }
}