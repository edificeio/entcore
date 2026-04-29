package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class RemoveUserGroupDTO {

    private String userId;
    private String groupId;

    public RemoveUserGroupDTO() {}

    public RemoveUserGroupDTO(JsonObject json) {
        RemoveUserGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        RemoveUserGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public RemoveUserGroupDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getGroupId() { return groupId; }
    public RemoveUserGroupDTO setGroupId(String groupId) { this.groupId = groupId; return this; }
}