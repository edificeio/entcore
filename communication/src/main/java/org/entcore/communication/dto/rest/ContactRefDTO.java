package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ContactRefDTO {

    private String id;
    private String displayName;

    public ContactRefDTO() {}

    public ContactRefDTO(JsonObject json) {
        this();
        ContactRefDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ContactRefDTOConverter.toJson(this, json);
        return json;
    }

    public String getId() {
        return id;
    }

    public ContactRefDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ContactRefDTO setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
}