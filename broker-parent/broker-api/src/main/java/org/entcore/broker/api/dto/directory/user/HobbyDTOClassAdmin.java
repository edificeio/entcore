package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
    * This DTO is used to represent user information for /directory/class-admin/:userId endpoint
    * it used in GetClassAdminResponseDTO to represent 
    * the response of the directory.class.admin.by.id nats subject
    * It is intended to be a duplicate of the others HobbyDTO as some informations
    * aren't return in the directory.class.admin.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class HobbyDTOClassAdmin {
    private final String visibility;
    private final String category;
    private final String values;

    @JsonCreator
    public HobbyDTOClassAdmin(
            @JsonProperty("visibility") String visibility,
            @JsonProperty("category") String category,
            @JsonProperty("values") String values) {
        this.visibility = visibility;
        this.category = category;
        this.values = values;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getCategory() {
        return category;
    }

    public String getValues() {
        return values;
    }
}