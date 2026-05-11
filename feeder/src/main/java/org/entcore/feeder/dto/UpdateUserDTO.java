package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateUserDTO {

    private String userId;
    private String callerId;
    private UpdateUserDataDTO data;

    public UpdateUserDTO() {}

    public UpdateUserDTO(JsonObject json) {
        UpdateUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateUserDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public UpdateUserDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getCallerId() { return callerId; }
    public UpdateUserDTO setCallerId(String callerId) { this.callerId = callerId; return this; }

    public UpdateUserDataDTO getData() { return data; }
    public UpdateUserDTO setData(UpdateUserDataDTO data) { this.data = data; return this; }
}