package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
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

    public SearchVisibleRequestDTO() {}

    public SearchVisibleRequestDTO(JsonObject json) {
        this();
        SearchVisibleRequestDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SearchVisibleRequestDTOConverter.toJson(this, json);
        return json;
    }

    /**
     * Returns this instance after applying server-side overrides for the REST visible endpoint:
     * {@code itSelf=true}, {@code myGroup=true}, {@code profile=false}.
     */
    public SearchVisibleRequestDTO withServerDefaults() {
        return setItSelf(true).setMyGroup(true).setProfile(false);
    }

    public List<String> getStructures() { return structures; }
    public SearchVisibleRequestDTO setStructures(List<String> structures) { this.structures = structures; return this; }

    public List<String> getClasses() { return classes; }
    public SearchVisibleRequestDTO setClasses(List<String> classes) { this.classes = classes; return this; }

    public List<String> getProfiles() { return profiles; }
    public SearchVisibleRequestDTO setProfiles(List<String> profiles) { this.profiles = profiles; return this; }

    public List<String> getFunctions() { return functions; }
    public SearchVisibleRequestDTO setFunctions(List<String> functions) { this.functions = functions; return this; }

    public List<String> getPositions() { return positions; }
    public SearchVisibleRequestDTO setPositions(List<String> positions) { this.positions = positions; return this; }

    public String getSearch() { return search; }
    public SearchVisibleRequestDTO setSearch(String search) { this.search = search; return this; }

    public List<String> getTypes() { return types; }
    public SearchVisibleRequestDTO setTypes(List<String> types) { this.types = types; return this; }

    public Boolean getNbUsersInGroups() { return nbUsersInGroups; }
    public SearchVisibleRequestDTO setNbUsersInGroups(Boolean nbUsersInGroups) { this.nbUsersInGroups = nbUsersInGroups; return this; }

    public Boolean getGroupType() { return groupType; }
    public SearchVisibleRequestDTO setGroupType(Boolean groupType) { this.groupType = groupType; return this; }

    public boolean isItSelf() { return itSelf; }
    public SearchVisibleRequestDTO setItSelf(boolean itSelf) { this.itSelf = itSelf; return this; }

    public boolean isMyGroup() { return myGroup; }
    public SearchVisibleRequestDTO setMyGroup(boolean myGroup) { this.myGroup = myGroup; return this; }

    /** Defaults to {@code true} when absent from the JSON payload. */
    public boolean isProfile() { return profile; }
    public SearchVisibleRequestDTO setProfile(boolean profile) { this.profile = profile; return this; }
}