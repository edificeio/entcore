package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverVisibleFilterDTO {

    private List<String> structures;
    private List<String> profiles;
    private String search;

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