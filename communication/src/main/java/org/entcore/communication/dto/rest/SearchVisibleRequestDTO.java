package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchVisibleRequestDTO {

    private List<String> structures;
    private List<String> classes;
    private List<String> profiles;
    private List<String> functions;
    private List<String> positions;
    private String search;
    private List<String> types;
    private Boolean nbUsersInGroups;
    private Boolean groupType;
    private boolean itSelf;
    private boolean myGroup;
    private boolean profile = true;

    public List<String> getStructures() {
        return structures;
    }

    public SearchVisibleRequestDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }

    public List<String> getClasses() {
        return classes;
    }

    public SearchVisibleRequestDTO setClasses(List<String> classes) {
        this.classes = classes;
        return this;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public SearchVisibleRequestDTO setProfiles(List<String> profiles) {
        this.profiles = profiles;
        return this;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public SearchVisibleRequestDTO setFunctions(List<String> functions) {
        this.functions = functions;
        return this;
    }

    public List<String> getPositions() {
        return positions;
    }

    public SearchVisibleRequestDTO setPositions(List<String> positions) {
        this.positions = positions;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public SearchVisibleRequestDTO setSearch(String search) {
        this.search = search;
        return this;
    }

    public List<String> getTypes() {
        return types;
    }

    public SearchVisibleRequestDTO setTypes(List<String> types) {
        this.types = types;
        return this;
    }

    public Boolean getNbUsersInGroups() {
        return nbUsersInGroups;
    }

    public SearchVisibleRequestDTO setNbUsersInGroups(Boolean nbUsersInGroups) {
        this.nbUsersInGroups = nbUsersInGroups;
        return this;
    }

    public Boolean getGroupType() {
        return groupType;
    }

    public SearchVisibleRequestDTO setGroupType(Boolean groupType) {
        this.groupType = groupType;
        return this;
    }

    public boolean isItSelf() {
        return itSelf;
    }

    public SearchVisibleRequestDTO setItSelf(boolean itSelf) {
        this.itSelf = itSelf;
        return this;
    }

    public boolean isMyGroup() {
        return myGroup;
    }

    public SearchVisibleRequestDTO setMyGroup(boolean myGroup) {
        this.myGroup = myGroup;
        return this;
    }

    public boolean isProfile() {
        return profile;
    }

    public SearchVisibleRequestDTO setProfile(boolean profile) {
        this.profile = profile;
        return this;
    }

}
