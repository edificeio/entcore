package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/*
    * This DTO is used to represent user information for /directory/class/:classId/users endpoint
    * it used in GetUserInClassWithParamsResponseDTO to represent 
    * the response of the directory.class.by.id.with.params nats subject
    * It is intended to be a duplicate of the others UserDTO as some informations
    * aren't return in the directory.class.by.id.with.params subject 
    * and to avoid confusion between the different subjects responses
*/
public class UserProfileDTOInClass {
    private final String lastName;
    private final String firstName;
    private final String id;
    private final Boolean hasEmail;
    private final String login;
    private final String originalLogin;
    private final String activationCode;
    private final String displayName;
    private final String birthDate;
    private final String lastLogin;
    private final String ine;
    private final String type;
    private final Boolean blocked;
    private final String source;
    private final List<RelativeDTOInClass> relativeList;

    @JsonCreator
    public UserProfileDTOInClass(
            @JsonProperty("lastName") String lastName,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("id") String id,
            @JsonProperty("hasEmail") Boolean hasEmail,
            @JsonProperty("login") String login,
            @JsonProperty("originalLogin") String originalLogin,
            @JsonProperty("activationCode") String activationCode,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("birthDate") String birthDate,
            @JsonProperty("lastLogin") String lastLogin,
            @JsonProperty("ine") String ine,
            @JsonProperty("type") String type,
            @JsonProperty("blocked") Boolean blocked,
            @JsonProperty("source") String source,
            @JsonProperty("relativeList") List<RelativeDTOInClass> relativeList) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.id = id;
        this.hasEmail = hasEmail;
        this.login = login;
        this.originalLogin = originalLogin;
        this.activationCode = activationCode;
        this.displayName = displayName;
        this.birthDate = birthDate;
        this.lastLogin = lastLogin;
        this.ine = ine;
        this.type = type;
        this.blocked = blocked;
        this.source = source;
        this.relativeList = relativeList;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getId() {
        return id;
    }

    public Boolean getHasEmail() {
        return hasEmail;
    }

    public String getLogin() {
        return login;
    }

    public String getOriginalLogin() {
        return originalLogin;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public String getIne() {
        return ine;
    }

    public String getType() {
        return type;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public String getSource() {
        return source;
    }

    public List<RelativeInClassDTO> getRelativeList() {
        return relativeList;
    }
}