package org.entcore.broker.api.dto.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class represents a user session.
 * It contains comprehensive user information including personal details,
 * authorization data, and associated groups, classes, and structures.
 */
public class SessionDto {
    /**
     * The internal user ID.
     * It is a unique identifier for the user in the system.
     */
    private final String userId;
    
    /**
     * The external user ID.
     * This is an identifier that may be used in external systems.
     */
    private final String externalId;
    
    /**
     * The first name of the user.
     */
    private final String firstName;
    
    /**
     * The last name of the user.
     */
    private final String lastName;
    
    /**
     * The username of the user for display purposes.
     */
    private final String username;
    
    /**
     * The birth date of the user.
     */
    private final String birthDate;
    
    /**
     * The education level of the user.
     */
    private final String level;
    
    /**
     * The type or role of the user in the system.
     */
    private final String type;
    
    /**
     * The login identifier used by the user to authenticate.
     */
    private final String login;
    
    /**
     * The email address of the user.
     */
    private final String email;
    
    /**
     * The mobile phone number of the user.
     */
    private final String mobile;
    
    /**
     * A list of actions the user is authorized to perform (workflow rights).
     */
    private final List<ActionDto> authorizedActions;
    
    /**
     * A list of classes the user belongs to.
     */
    private final List<ClassDto> classes;
    
    /**
     * A list of groups the user belongs to.
     */
    private final List<GroupDto> groups;
    
    /**
     * A list of structures the user is associated with.
     */
    private final List<StructureDto> structures;

    /**
     * Creates a new instance of SessionDto with all user information.
     *
     * @param userId The internal user ID.
     * @param externalId The external user ID.
     * @param firstName The first name of the user.
     * @param lastName The last name of the user.
     * @param username The username for display purposes.
     * @param birthDate The birth date of the user.
     * @param level The education level of the user.
     * @param type The type or role of the user.
     * @param login The login identifier.
     * @param email The email address of the user.
     * @param mobile The mobile phone number of the user.
     * @param authorizedActions The list of actions the user is authorized to perform.
     * @param classes The list of classes the user belongs to.
     * @param groups The list of groups the user belongs to.
     * @param structures The list of structures the user is associated with.
     */
    @JsonCreator
    public SessionDto(
            @JsonProperty("userId") String userId,
            @JsonProperty("externalId") String externalId,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("username") String username,
            @JsonProperty("birthDate") String birthDate,
            @JsonProperty("level") String level,
            @JsonProperty("type") String type,
            @JsonProperty("login") String login,
            @JsonProperty("email") String email,
            @JsonProperty("mobile") String mobile,
            @JsonProperty("authorizedActions") List<ActionDto> authorizedActions,
            @JsonProperty("classes") List<ClassDto> classes,
            @JsonProperty("groups") List<GroupDto> groups,
            @JsonProperty("structures") List<StructureDto> structures) {
        this.userId = userId;
        this.externalId = externalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.birthDate = birthDate;
        this.level = level;
        this.type = type;
        this.login = login;
        this.email = email;
        this.mobile = mobile;
        this.authorizedActions = authorizedActions;
        this.classes = classes;
        this.groups = groups;
        this.structures = structures;
    }

    /**
     * Gets the internal user ID.
     *
     * @return The unique identifier for the user in the system.
     */
    public String getUserId() { return userId; }

    /**
     * Gets the external user ID.
     *
     * @return The identifier that may be used in external systems.
     */
    public String getExternalId() { return externalId; }

    /**
     * Gets the first name of the user.
     *
     * @return The first name of the user.
     */
    public String getFirstName() { return firstName; }

    /**
     * Gets the last name of the user.
     *
     * @return The last name of the user.
     */
    public String getLastName() { return lastName; }

    /**
     * Gets the username of the user.
     *
     * @return The username for display purposes.
     */
    public String getUsername() { return username; }

    /**
     * Gets the birth date of the user.
     *
     * @return The birth date of the user.
     */
    public String getBirthDate() { return birthDate; }

    /**
     * Gets the education level of the user.
     *
     * @return The education level of the user.
     */
    public String getLevel() { return level; }

    /**
     * Gets the type or role of the user in the system.
     *
     * @return The type or role of the user.
     */
    public String getType() { return type; }

    /**
     * Gets the login identifier of the user.
     *
     * @return The login identifier used for authentication.
     */
    public String getLogin() { return login; }

    /**
     * Gets the email address of the user.
     *
     * @return The email address of the user.
     */
    public String getEmail() { return email; }

    /**
     * Gets the mobile phone number of the user.
     *
     * @return The mobile phone number of the user.
     */
    public String getMobile() { return mobile; }

    /**
     * Gets the list of actions the user is authorized to perform.
     *
     * @return The list of authorized actions for the user.
     */
    public List<ActionDto> getAuthorizedActions() { return authorizedActions; }

    /**
     * Gets the list of classes the user belongs to.
     *
     * @return The list of classes associated with the user.
     */
    public List<ClassDto> getClasses() { return classes; }

    /**
     * Gets the list of groups the user belongs to.
     *
     * @return The list of groups associated with the user.
     */
    public List<GroupDto> getGroups() { return groups; }

    /**
     * Gets the list of structures the user is associated with.
     *
     * @return The list of structures associated with the user.
     */
    public List<StructureDto> getStructures() { return structures; }
    
}