package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddGroupUsersDTO {

    private String groupId;
    private List<String> userIds;

    public AddGroupUsersDTO() {}

    public AddGroupUsersDTO(JsonObject json) {
        AddGroupUsersDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddGroupUsersDTOConverter.toJson(this, json);
        return json;
    }

    public String getGroupId() { return groupId; }
    public AddGroupUsersDTO setGroupId(String groupId) { this.groupId = groupId; return this; }

    public List<String> getUserIds() { return userIds; }
    public AddGroupUsersDTO setUserIds(List<String> userIds) { this.userIds = userIds; return this; }
}