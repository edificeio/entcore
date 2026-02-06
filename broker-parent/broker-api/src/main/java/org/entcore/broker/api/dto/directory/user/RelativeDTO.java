package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RelativeDTO {
    private final String relatedName;
    private final String relatedType;
    private final String relatedId;

    @JsonCreator
    public RelativeDTO(
            @JsonProperty("relatedName") String relatedName,
            @JsonProperty("relatedType") String relatedType,
            @JsonProperty("relatedId") String relatedId) {
        this.relatedName = relatedName;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    public String getRelatedName() {
        return relatedName;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public String getRelatedId() {
        return relatedId;
    }
}