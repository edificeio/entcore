package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a group of users in the system.
 * It contains the unique identifier and name of the group.
 */
public class GroupDto {
    /**
     * The ID of the group.
     * It is a unique identifier for the group in the directory.
     */
    private final String id;
    
    /**
     * The name of the group.
     * This is the display name shown to users.
     */
    private final String name;

    /**
     * Creates a new instance of GroupDto.
     *
     * @param id The unique identifier of the group.
     * @param name The name of the group.
     */
    @JsonCreator
    public GroupDto(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the ID of the group.
     *
     * @return The unique identifier of the group in the directory.
     */
    public String getId() { return id; }

    /**
     * Gets the name of the group.
     *
     * @return The display name of the group shown to users.
     */
    public String getName() { return name; }
}