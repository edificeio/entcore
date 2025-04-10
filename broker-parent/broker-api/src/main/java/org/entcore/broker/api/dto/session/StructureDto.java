package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents an educational structure such as a school.
 * It contains the unique identifier and name of the structure.
 */
public class StructureDto {
    /**
     * The ID of the structure.
     * It is a unique identifier for the structure in the system.
     */
    private final String id;
    
    /**
     * The name of the structure.
     * This is the display name of the structure shown to users.
     */
    private final String name;

    /**
     * Creates a new instance of StructureDto.
     *
     * @param id The unique identifier of the structure.
     * @param name The name of the structure.
     */
    @JsonCreator
    public StructureDto(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the ID of the structure.
     *
     * @return The unique identifier of the structure in the system.
     */
    public String getId() { return id; }

    /**
     * Gets the name of the structure.
     *
     * @return The display name of the structure shown to users.
     */
    public String getName() { return name; }
}