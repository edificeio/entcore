package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO extends IdentifiableDTO {

    private String displayName;
    private String groupDisplayName;
    private String profile;
    private String type;
    private String login;
    private List<String> subjects;
    private List<IdentifiableDTO> positions;
    private List<String> structures;
    private Boolean hasCommunication;

    public String getDisplayName() {
        return displayName;
    }

    public UserDTO setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getGroupDisplayName() {
        return groupDisplayName;
    }

    public UserDTO setGroupDisplayName(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        return this;
    }

    public String getProfile() {
        return profile;
    }

    public UserDTO setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public UserDTO setSubjects(List<String> subjects) {
        this.subjects = subjects;
        return this;
    }

    public List<IdentifiableDTO> getPositions() {
        return positions;
    }

    public UserDTO setPositions(List<IdentifiableDTO> positions) {
        this.positions = positions;
        return this;
    }

    public String getType() {
        return type;
    }

    public UserDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public UserDTO setLogin(String login) {
        this.login = login;
        return this;
    }

    public List<String> getStructures() {
        return structures;
    }

    public UserDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }

    public Boolean getHasCommunication() {
        return hasCommunication;
    }

    public UserDTO setHasCommunication(Boolean hasCommunication) {
        this.hasCommunication = hasCommunication;
        return this;
    }
}
