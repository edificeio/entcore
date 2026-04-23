package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchVisibleContactDTO {

    private String id;
    private String displayName;
    private String type;
    private List<String> usedIn;

    // users and ProfileGroup
    private String profile;

    // groups
    private String groupType;
    private Integer nbUsers;
    private String structureName;

    // non-student users
    private List<ContactRefDTO> children;
    private List<ContactRefDTO> relatives;

    public String getId() {
        return id;
    }

    public SearchVisibleContactDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SearchVisibleContactDTO setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getType() {
        return type;
    }

    public SearchVisibleContactDTO setType(String type) {
        this.type = type;
        return this;
    }

    public List<String> getUsedIn() {
        return usedIn;
    }

    public SearchVisibleContactDTO setUsedIn(List<String> usedIn) {
        this.usedIn = usedIn;
        return this;
    }

    public String getProfile() {
        return profile;
    }

    public SearchVisibleContactDTO setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public String getGroupType() {
        return groupType;
    }

    public SearchVisibleContactDTO setGroupType(String groupType) {
        this.groupType = groupType;
        return this;
    }

    public Integer getNbUsers() {
        return nbUsers;
    }

    public SearchVisibleContactDTO setNbUsers(Integer nbUsers) {
        this.nbUsers = nbUsers;
        return this;
    }

    public String getStructureName() {
        return structureName;
    }

    public SearchVisibleContactDTO setStructureName(String structureName) {
        this.structureName = structureName;
        return this;
    }

    public List<ContactRefDTO> getChildren() {
        return children;
    }

    public SearchVisibleContactDTO setChildren(List<ContactRefDTO> children) {
        this.children = children;
        return this;
    }

    public List<ContactRefDTO> getRelatives() {
        return relatives;
    }

    public SearchVisibleContactDTO setRelatives(List<ContactRefDTO> relatives) {
        this.relatives = relatives;
        return this;
    }
}