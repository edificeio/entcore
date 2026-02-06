package org.entcore.broker.api.dto.directory.clazz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassDTO {
    private final String name;
    private final String id;

    @JsonCreator
    public ClassDTO(
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