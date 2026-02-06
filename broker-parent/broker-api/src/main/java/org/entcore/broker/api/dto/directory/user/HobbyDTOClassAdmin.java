package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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