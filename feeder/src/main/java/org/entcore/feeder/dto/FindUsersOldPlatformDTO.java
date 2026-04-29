package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class FindUsersOldPlatformDTO {

    private JsonObject matcher;
    private JsonObject update;
    private JsonObject sort;
    private JsonObject keys;

    public FindUsersOldPlatformDTO() {}

    public FindUsersOldPlatformDTO(JsonObject json) {
        FindUsersOldPlatformDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        FindUsersOldPlatformDTOConverter.toJson(this, json);
        return json;
    }

    public JsonObject getMatcher() { return matcher; }
    public FindUsersOldPlatformDTO setMatcher(JsonObject matcher) { this.matcher = matcher; return this; }

    public JsonObject getUpdate() { return update; }
    public FindUsersOldPlatformDTO setUpdate(JsonObject update) { this.update = update; return this; }

    public JsonObject getSort() { return sort; }
    public FindUsersOldPlatformDTO setSort(JsonObject sort) { this.sort = sort; return this; }

    public JsonObject getKeys() { return keys; }
    public FindUsersOldPlatformDTO setKeys(JsonObject keys) { this.keys = keys; return this; }
}