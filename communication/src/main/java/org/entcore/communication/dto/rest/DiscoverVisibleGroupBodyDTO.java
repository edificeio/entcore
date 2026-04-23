package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverVisibleGroupBodyDTO {

    private String name;

    public String getName() {
        return name;
    }

    public DiscoverVisibleGroupBodyDTO setName(String name) {
        this.name = name;
        return this;
    }
}