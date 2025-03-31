package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents an action that can be performed by a user.
 * It contains information about the type, name, and display name of the action.
 */
public class ActionDto {
    /**
     * The type of the action.
     */
    private final String type;
    
    /**
     * The name of the action.
     */
    private final String name;
    
    /**
     * The display name of the action.
     * This is the user-friendly name shown in the UI.
     */
    private final String displayName;

    /**
     * Creates a new instance of ActionDto.
     *
     * @param type The type of the action.
     * @param name The name of the action.
     * @param displayName The display name of the action.
     */
    @JsonCreator
    public ActionDto(
            @JsonProperty("type") String type,
            @JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName) {
        this.type = type;
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * Gets the type of the action.
     *
     * @return The type of the action.
     */
    public String getType() { return type; }

    /**
     * Gets the name of the action.
     *
     * @return The name of the action which is a unique identifier.
     */
    public String getName() { return name; }

    /**
     * Gets the display name of the action.
     *
     * @return The user-friendly name of the action shown in the UI.
     */
    public String getDisplayName() { return displayName; }
}