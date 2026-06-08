package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen(inheritConverter = true)
public class CommuniqueWithDTO extends GroupDTO {

    private List<GroupDTO> communiqueWith;

    public CommuniqueWithDTO() {}

    public CommuniqueWithDTO(JsonObject json) {
        this();
        CommuniqueWithDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CommuniqueWithDTOConverter.toJson(this, json);
        return json;
    }

    public List<GroupDTO> getCommuniqueWith() {
        return communiqueWith;
    }

    public CommuniqueWithDTO setCommuniqueWith(List<GroupDTO> communiqueWith) {
        this.communiqueWith = communiqueWith;
        return this;
    }
}