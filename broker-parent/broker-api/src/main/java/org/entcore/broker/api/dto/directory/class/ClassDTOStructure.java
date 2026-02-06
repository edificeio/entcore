package org.entcore.broker.api.dto.directory.clazz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
    * This DTO is used to represent class information
    * it used in UserProfileDTOStructure to represent 
    * the response of the directory.structure.users.by.id nats subject
    * It is intended to be a duplicate of the others ClassDTO as some informations
    * aren't return in the directory.structure.users.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class ClassDTOStructure {
    private final String name;
    private final String externalId;
    private final String id;

    @JsonCreator
    public ClassDTOStructure(
            @JsonProperty("name") String name,
            @JsonProperty("externalId") String externalId,
            @JsonProperty("id") String id) {
        this.name = name;
        this.externalId = externalId;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getId() {
        return id;
    }
}