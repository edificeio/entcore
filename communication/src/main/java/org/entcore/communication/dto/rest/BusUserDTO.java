package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusUserDTO {

    private String id;
    private String name;
    private String login;
    private String username;
    private String type;
    private String profile;
    private String lastName;
    private String firstName;
    private List<String> profiles;
    private List<String> positionIds;
    private List<String> positionNames;

    public String getId() { return id; }
    public BusUserDTO setId(String id) { this.id = id; return this; }

    public String getName() { return name; }
    public BusUserDTO setName(String name) { this.name = name; return this; }

    public String getLogin() { return login; }
    public BusUserDTO setLogin(String login) { this.login = login; return this; }

    public String getUsername() { return username; }
    public BusUserDTO setUsername(String username) { this.username = username; return this; }

    public String getType() { return type; }
    public BusUserDTO setType(String type) { this.type = type; return this; }

    public String getProfile() { return profile; }
    public BusUserDTO setProfile(String profile) { this.profile = profile; return this; }

    public String getLastName() { return lastName; }
    public BusUserDTO setLastName(String lastName) { this.lastName = lastName; return this; }

    public String getFirstName() { return firstName; }
    public BusUserDTO setFirstName(String firstName) { this.firstName = firstName; return this; }

    public List<String> getProfiles() { return profiles; }
    public BusUserDTO setProfiles(List<String> profiles) { this.profiles = profiles; return this; }

    public List<String> getPositionIds() { return positionIds; }
    public BusUserDTO setPositionIds(List<String> positionIds) { this.positionIds = positionIds; return this; }

    public List<String> getPositionNames() { return positionNames; }
    public BusUserDTO setPositionNames(List<String> positionNames) { this.positionNames = positionNames; return this; }
}