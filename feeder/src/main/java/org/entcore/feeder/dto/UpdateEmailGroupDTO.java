package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateEmailGroupDTO {

    private String groupId;
    private String email;

    public UpdateEmailGroupDTO() {}

    public UpdateEmailGroupDTO(JsonObject json) {
        UpdateEmailGroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateEmailGroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getGroupId() { return groupId; }
    public UpdateEmailGroupDTO setGroupId(String groupId) { this.groupId = groupId; return this; }

    public String getEmail() { return email; }
    public UpdateEmailGroupDTO setEmail(String email) { this.email = email; return this; }
}