package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateFunctionDTO {

    private String profile;
    private String externalId;
    private String name;

    public CreateFunctionDTO() {}

    public CreateFunctionDTO(JsonObject json) {
        CreateFunctionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateFunctionDTOConverter.toJson(this, json);
        return json;
    }

    public String getProfile() { return profile; }
    public CreateFunctionDTO setProfile(String profile) { this.profile = profile; return this; }

    public String getExternalId() { return externalId; }
    public CreateFunctionDTO setExternalId(String externalId) { this.externalId = externalId; return this; }

    public String getName() { return name; }
    public CreateFunctionDTO setName(String name) { this.name = name; return this; }
}
