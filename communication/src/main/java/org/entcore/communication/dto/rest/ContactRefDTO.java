package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactRefDTO {

    private String id;
    private String displayName;

    public String getId() {
        return id;
    }

    public ContactRefDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ContactRefDTO setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
}