package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchVisibleRestDTO {

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

    public SearchVisibleRestDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }

    public List<String> getClasses() {
        return classes;
    }

    public SearchVisibleRestDTO setClasses(List<String> classes) {
        this.classes = classes;
        return this;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public SearchVisibleRestDTO setProfiles(List<String> profiles) {
        this.profiles = profiles;
        return this;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public SearchVisibleRestDTO setFunctions(List<String> functions) {
        this.functions = functions;
        return this;
    }

    public List<String> getPositions() {
        return positions;
    }

    public SearchVisibleRestDTO setPositions(List<String> positions) {
        this.positions = positions;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public SearchVisibleRestDTO setSearch(String search) {
        this.search = search;
        return this;
    }

    public List<String> getTypes() {
        return types;
    }

    public SearchVisibleRestDTO setTypes(List<String> types) {
        this.types = types;
        return this;
    }

    public Boolean getNbUsersInGroups() {
        return nbUsersInGroups;
    }

    public SearchVisibleRestDTO setNbUsersInGroups(Boolean nbUsersInGroups) {
        this.nbUsersInGroups = nbUsersInGroups;
        return this;
    }

    public Boolean getGroupType() {
        return groupType;
    }

    public SearchVisibleRestDTO setGroupType(Boolean groupType) {
        this.groupType = groupType;
        return this;
    }

    public boolean isItSelf() {
        return itSelf;
    }

    public SearchVisibleRestDTO setItSelf(boolean itSelf) {
        this.itSelf = itSelf;
        return this;
    }

    public boolean isMyGroup() {
        return myGroup;
    }

    public SearchVisibleRestDTO setMyGroup(boolean myGroup) {
        this.myGroup = myGroup;
        return this;
    }

    public boolean isProfile() {
        return profile;
    }

    public SearchVisibleRestDTO setProfile(boolean profile) {
        this.profile = profile;
        return this;
    }

}
