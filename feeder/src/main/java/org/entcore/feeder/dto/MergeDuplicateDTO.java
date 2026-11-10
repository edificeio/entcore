package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class MergeDuplicateDTO {

    private String userId1;
    private String userId2;
    private Boolean keepRelations;

    public MergeDuplicateDTO() {}

    public MergeDuplicateDTO(JsonObject json) {
        MergeDuplicateDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        MergeDuplicateDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId1() { return userId1; }
    public MergeDuplicateDTO setUserId1(String userId1) { this.userId1 = userId1; return this; }

    public String getUserId2() { return userId2; }
    public MergeDuplicateDTO setUserId2(String userId2) { this.userId2 = userId2; return this; }

    public Boolean getKeepRelations() { return keepRelations; }
    public MergeDuplicateDTO setKeepRelations(Boolean keepRelations) { this.keepRelations = keepRelations; return this; }
}