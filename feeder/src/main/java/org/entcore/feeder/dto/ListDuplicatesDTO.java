package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class ListDuplicatesDTO {

    private List<String> structures;
    private Boolean inherit;
    private Integer minScore;
    private Boolean inSameStructure;

    public ListDuplicatesDTO() {}

    public ListDuplicatesDTO(JsonObject json) {
        ListDuplicatesDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ListDuplicatesDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getStructures() { return structures; }
    public ListDuplicatesDTO setStructures(List<String> structures) { this.structures = structures; return this; }

    public Boolean getInherit() { return inherit; }
    public ListDuplicatesDTO setInherit(Boolean inherit) { this.inherit = inherit; return this; }

    public Integer getMinScore() { return minScore; }
    public ListDuplicatesDTO setMinScore(Integer minScore) { this.minScore = minScore; return this; }

    public Boolean getInSameStructure() { return inSameStructure; }
    public ListDuplicatesDTO setInSameStructure(Boolean inSameStructure) { this.inSameStructure = inSameStructure; return this; }
}