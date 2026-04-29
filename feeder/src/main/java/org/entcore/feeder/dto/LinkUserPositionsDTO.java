package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class LinkUserPositionsDTO {

    private String groupId;
    private List<String> manualGroupAutolinkUsersPositions;

    public LinkUserPositionsDTO() {}

    public LinkUserPositionsDTO(JsonObject json) {
        LinkUserPositionsDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        LinkUserPositionsDTOConverter.toJson(this, json);
        return json;
    }

    public String getGroupId() { return groupId; }
    public LinkUserPositionsDTO setGroupId(String groupId) { this.groupId = groupId; return this; }

    public List<String> getManualGroupAutolinkUsersPositions() { return manualGroupAutolinkUsersPositions; }
    public LinkUserPositionsDTO setManualGroupAutolinkUsersPositions(List<String> manualGroupAutolinkUsersPositions) { this.manualGroupAutolinkUsersPositions = manualGroupAutolinkUsersPositions; return this; }
}