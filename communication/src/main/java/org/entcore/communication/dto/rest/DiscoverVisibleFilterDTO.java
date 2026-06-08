package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverVisibleFilterDTO {

    private List<String> structures;
    private List<String> profiles;
    private String search;

    public DiscoverVisibleFilterDTO() {}

    public DiscoverVisibleFilterDTO(JsonObject json) {
        this();
        DiscoverVisibleFilterDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DiscoverVisibleFilterDTOConverter.toJson(this, json);
        return json;
    }

    public List<String> getStructures() {
        return structures;
    }

    public DiscoverVisibleFilterDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public DiscoverVisibleFilterDTO setProfiles(List<String> profiles) {
        this.profiles = profiles;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public DiscoverVisibleFilterDTO setSearch(String search) {
        this.search = search;
        return this;
    }
}