package org.entcore.communication.dto.rest;

public class IdentifiableDTO {

    private String id;
    private String name;

    public IdentifiableDTO(){
        //for serialization
    }

    public IdentifiableDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public IdentifiableDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public IdentifiableDTO setName(String name) {
        this.name = name;
        return this;
    }
}
