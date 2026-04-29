package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UserDataDTO {

    private String firstName;
    private String lastName;
    private String birthDate;
    private String externalId;
    private String email;
    private String emailAcademy;
    private String source;
    private String profile;
    private List<String> profiles;
    private String type;
    private List<String> childrenIds;
    private List<String> userPositionIds;

    public UserDataDTO() {}

    public UserDataDTO(JsonObject json) {
        UserDataDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UserDataDTOConverter.toJson(this, json);
        return json;
    }

    public String getFirstName() { return firstName; }
    public UserDataDTO setFirstName(String firstName) { this.firstName = firstName; return this; }

    public String getLastName() { return lastName; }
    public UserDataDTO setLastName(String lastName) { this.lastName = lastName; return this; }

    public String getBirthDate() { return birthDate; }
    public UserDataDTO setBirthDate(String birthDate) { this.birthDate = birthDate; return this; }

    public String getExternalId() { return externalId; }
    public UserDataDTO setExternalId(String externalId) { this.externalId = externalId; return this; }

    public String getEmail() { return email; }
    public UserDataDTO setEmail(String email) { this.email = email; return this; }

    public String getEmailAcademy() { return emailAcademy; }
    public UserDataDTO setEmailAcademy(String emailAcademy) { this.emailAcademy = emailAcademy; return this; }

    public String getSource() { return source; }
    public UserDataDTO setSource(String source) { this.source = source; return this; }

    public String getProfile() { return profile; }
    public UserDataDTO setProfile(String profile) { this.profile = profile; return this; }

    public List<String> getProfiles() { return profiles; }
    public UserDataDTO setProfiles(List<String> profiles) { this.profiles = profiles; return this; }

    public String getType() { return type; }
    public UserDataDTO setType(String type) { this.type = type; return this; }

    public List<String> getChildrenIds() { return childrenIds; }
    public UserDataDTO setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; return this; }

    public List<String> getUserPositionIds() { return userPositionIds; }
    public UserDataDTO setUserPositionIds(List<String> userPositionIds) { this.userPositionIds = userPositionIds; return this; }
}