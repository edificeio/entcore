package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class DeleteFunctionGroupDTO {

    private String groupId;

    public DeleteFunctionGroupDTO() {}

    public DeleteFunctionGroupDTO(JsonObject json) {
        DeleteFunctionGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteFunctionGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getGroupId() { return groupId; }
    public DeleteFunctionGroupDTO setGroupId(String groupId) { this.groupId = groupId; return this; }
}
