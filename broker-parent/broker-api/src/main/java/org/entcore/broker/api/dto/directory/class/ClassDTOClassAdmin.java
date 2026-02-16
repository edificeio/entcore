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
    private final String inc;
    private final String level;

    @JsonCreator
    public ClassDTOClassAdmin(
            @JsonProperty("name") String name,
            @JsonProperty("id") String id,
            @JsonProperty("inc") String inc,
            @JsonProperty("level") String level) {
        this.name = name;
        this.id = id;
        this.inc = inc;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getInc() {
        return inc;
    }

    public String getLevel() {
        return level;
    }
}