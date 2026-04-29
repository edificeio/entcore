package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class IgnoreDuplicateDTO {

    private String userId1;
    private String userId2;

    public IgnoreDuplicateDTO() {}

    public IgnoreDuplicateDTO(JsonObject json) {
        IgnoreDuplicateDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        IgnoreDuplicateDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId1() { return userId1; }
    public IgnoreDuplicateDTO setUserId1(String userId1) { this.userId1 = userId1; return this; }

    public String getUserId2() { return userId2; }
    public IgnoreDuplicateDTO setUserId2(String userId2) { this.userId2 = userId2; return this; }
}