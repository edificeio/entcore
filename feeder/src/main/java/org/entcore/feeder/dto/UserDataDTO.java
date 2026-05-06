package org.entcore.feeder.dto;

import java.util.List;

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

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmailAcademy() { return emailAcademy; }
    public void setEmailAcademy(String emailAcademy) { this.emailAcademy = emailAcademy; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public List<String> getProfiles() { return profiles; }
    public void setProfiles(List<String> profiles) { this.profiles = profiles; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }

    public List<String> getUserPositionIds() { return userPositionIds; }
    public void setUserPositionIds(List<String> userPositionIds) { this.userPositionIds = userPositionIds; }
}