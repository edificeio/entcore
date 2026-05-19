package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class IdentifiableDTO {

    private String id;
    private String name;

    public IdentifiableDTO() {}

    public IdentifiableDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public IdentifiableDTO(JsonObject json) {
        this();
        IdentifiableDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        IdentifiableDTOConverter.toJson(this, json);
        return json;
    }

    public String getId() {
        return id;
    }

    public IdentifiableDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public IdentifiableDTO setName(String name) {
        this.name = name;
        return this;
    }
}