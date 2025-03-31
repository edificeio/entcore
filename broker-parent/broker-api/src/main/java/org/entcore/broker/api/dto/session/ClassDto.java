package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a school class.
 * It contains the unique identifier and name of the class.
 */
public class ClassDto {
    /**
     * The ID of the class.
     * It is a unique identifier for the class in the system.
     */
    private final String id;
    
    /**
     * The name of the class.
     * This is the display name shown to users.
     */
    private final String name;

    /**
     * Creates a new instance of ClassDto.
     *
     * @param id The unique identifier of the class.
     * @param name The name of the class.
     */
    @JsonCreator
    public ClassDto(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the ID of the class.
     *
     * @return The unique identifier of the class in the system.
     */
    public String getId() { return id; }

    /**
     * Gets the name of the class.
     *
     * @return The display name of the class shown to users.
     */
    public String getName() { return name; }
}