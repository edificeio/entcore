package org.entcore.communication.dto.rest;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
public class InitDefaultRulesDTO {

    private List<String> structures;

    public InitDefaultRulesDTO() {}

    public InitDefaultRulesDTO(JsonObject json) {
        this();
        InitDefaultRulesDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        InitDefaultRulesDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getStructures() {
        return structures;
    }

    public InitDefaultRulesDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }
}