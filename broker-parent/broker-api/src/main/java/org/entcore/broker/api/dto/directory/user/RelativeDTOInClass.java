package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/*
    * This DTO is used to represent user information for /directory/class/:classId/users endpoint
    * it used in GetUserInClassWithParamsResponseDTO to represent 
    * the response of the directory.class.by.id.with.params nats subject
    * It is intended to be a duplicate of the others RelativeDTO as some informations
    * aren't return in the directory.class.by.id.with.params subject 
    * and to avoid confusion between the different subjects responses
*/
public class RelativeDTOInClass {
    private final String relatedName;
    private final List<String> relatedType;
    private final String relatedId;

    @JsonCreator
    public RelativeDTOInClass(
            @JsonProperty("relatedName") String relatedName,
            @JsonProperty("relatedType") List<String> relatedType,
            @JsonProperty("relatedId") String relatedId) {
        this.relatedName = relatedName;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    public String getRelatedName() {
        return relatedName;
    }

    public List<String> getRelatedType() {
        return relatedType;
    }

    public String getRelatedId() {
        return relatedId;
    }
}