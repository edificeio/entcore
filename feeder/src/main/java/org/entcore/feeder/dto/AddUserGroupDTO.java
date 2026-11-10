package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddUserGroupDTO {

    private String userId;
    private String groupId;

    public AddUserGroupDTO() {}

    public AddUserGroupDTO(JsonObject json) {
        AddUserGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddUserGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public AddUserGroupDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getGroupId() { return groupId; }
    public AddUserGroupDTO setGroupId(String groupId) { this.groupId = groupId; return this; }
}