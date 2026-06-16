package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

@DataObject
@JsonGen
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverModifyGroupUsersDTO {

    private List<String> oldUsers = new ArrayList<>();
    private List<String> newUsers;

    public DiscoverModifyGroupUsersDTO() {}

    public DiscoverModifyGroupUsersDTO(JsonObject json) {
        this();
        DiscoverModifyGroupUsersDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DiscoverModifyGroupUsersDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getOldUsers() {
        return oldUsers;
    }

    public DiscoverModifyGroupUsersDTO setOldUsers(List<String> oldUsers) {
        this.oldUsers = oldUsers != null ? oldUsers : new ArrayList<>();
        return this;
    }

    public List<String> getNewUsers() {
        return newUsers;
    }

    public DiscoverModifyGroupUsersDTO setNewUsers(List<String> newUsers) {
        this.newUsers = newUsers;
        return this;
    }
}