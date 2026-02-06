package org.entcore.broker.api.dto.directory.clazz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
    * This DTO is used to represent class information for class-admin
    * it used in GetClassAdminResponseDTO to represent 
    * the response of the directory.class.admin.by.id nats subject
    * It is intended to be a duplicate of the others ClassDTO as some informations
    * aren't return in the directory.class.admin.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class ClassDTOClassAdmin {
    private final String name;
    private final String id;

    @JsonCreator
    public ClassDTOClassAdmin(
            @JsonProperty("name") String name,
            @JsonProperty("id") String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}