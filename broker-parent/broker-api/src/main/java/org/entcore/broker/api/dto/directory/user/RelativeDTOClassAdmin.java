package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
    * This DTO is used to represent user information for /directory/class-admin/:userId endpoint
    * it used in GetClassAdminResponseDTO to represent 
    * the response of the directory.class.admin.by.id nats subject
    * It is intended to be a duplicate of the others RelativeDTO as some informations
    * aren't return in the directory.class.admin.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class RelativeDTOClassAdmin {
    private final String relatedName;
    private final String relatedType;
    private final String relatedId;

    @JsonCreator
    public RelativeDTOClassAdmin(
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