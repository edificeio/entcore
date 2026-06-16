package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverVisibleGroupBodyDTO {

    private String name;

    public DiscoverVisibleGroupBodyDTO() {}

    public DiscoverVisibleGroupBodyDTO(JsonObject json) {
        this();
        DiscoverVisibleGroupBodyDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DiscoverVisibleGroupBodyDTOConverter.toJson(this, json);
        return json;
    }

    public String getName() {
        return name;
    }

    public DiscoverVisibleGroupBodyDTO setName(String name) {
        this.name = name;
        return this;
    }
}