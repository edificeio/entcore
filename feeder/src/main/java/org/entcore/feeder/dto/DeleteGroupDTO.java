package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class DeleteGroupDTO {

    private String groupId;

    public DeleteGroupDTO() {}

    public DeleteGroupDTO(JsonObject json) {
        DeleteGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getGroupId() { return groupId; }
    public DeleteGroupDTO setGroupId(String groupId) { this.groupId = groupId; return this; }
}