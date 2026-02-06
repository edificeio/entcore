package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.structure.SchoolDTO;
import java.util.List;

/*
    * This DTO is used to represent user information for class-admin
    * it used in GetClassAdminResponseDTO to represent 
    * the response of the directory.class.admin.by.id nats subject
    * It is intended to be a duplicate of the others UserDTO as some informations
    * aren't return in the directory.class.admin.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class UserProfileDTOClassAdmin {
    private final List<String> profiles; // Teacher, Student, Personnal, ...
    private final String id; // The ENT ID of the user
    private final String firstName; 
    private final String lastName;
    private final String displayName; // Generally, the concatenation of firstName and lastName
    private final String email;
    private final String homePhone;
    private final String mobile;
    private final String birthDate; // Format: "yyyy-MM-dd"
    private final String originalLogin; // firstname.lastname or firstname.lastname1, etc. depending on the number of users with the same name in the directory
    private final List<RelativeDTOClassAdmin> relativeList; // List of relatives (parents, tutors, etc.) with their ENT ID, name and type of relationship
    private final String motto; 
    private final String health;
    private final String mood;
    private final List<HobbyDTOClassAdmin> hobbies;
    private final List<SchoolDTOClassAdmin> schools; // List of schools the user is associated with, each containing a list of classes the user is enrolled in or teaches
    private final Boolean lockedEmail;

    @JsonCreator
    public UserProfileDTOClassAdmin(
            @JsonProperty("profiles") List<String> profiles,
            @JsonProperty("id") String id,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("email") String email,
            @JsonProperty("homePhone") String homePhone,
            @JsonProperty("mobile") String mobile,
            @JsonProperty("birthDate") String birthDate,
            @JsonProperty("originalLogin") String originalLogin,
            @JsonProperty("relativeList") List<RelativeDTOClassAdmin> relativeList,
            @JsonProperty("motto") String motto,
            @JsonProperty("health") String health,
            @JsonProperty("mood") String mood,
            @JsonProperty("hobbies") List<HobbyDTOClassAdmin> hobbies,
            @JsonProperty("schools") List<SchoolDTOClassAdmin> schools,
            @JsonProperty("lockedEmail") Boolean lockedEmail) {
        this.profiles = profiles;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.email = email;
        this.homePhone = homePhone;
        this.mobile = mobile;
        this.birthDate = birthDate;
        this.originalLogin = originalLogin;
        this.relativeList = relativeList;
        this.motto = motto;
        this.health = health;
        this.mood = mood;
        this.hobbies = hobbies;
        this.schools = schools;
        this.lockedEmail = lockedEmail;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public String getMobile() {
        return mobile;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getOriginalLogin() {
        return originalLogin;
    }

    public List<RelativeDTOClassAdmin> getRelativeList() {
        return relativeList;
    }

    public String getMotto() {
        return motto;
    }

    public String getHealth() {
        return health;
    }

    public String getMood() {
        return mood;
    }

    public List<HobbyDTOClassAdmin> getHobbies() {
        return hobbies;
    }

    public List<SchoolDTOClassAdmin> getSchools() {
        return schools;
    }

    public Boolean getLockedEmail() {
        return lockedEmail;
    }
}
