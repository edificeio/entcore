package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CheckDuplicatesIntegrityDTO {

    private String userId;

    public CheckDuplicatesIntegrityDTO() {}

    public CheckDuplicatesIntegrityDTO(JsonObject json) {
        CheckDuplicatesIntegrityDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CheckDuplicatesIntegrityDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public CheckDuplicatesIntegrityDTO setUserId(String userId) { this.userId = userId; return this; }
}