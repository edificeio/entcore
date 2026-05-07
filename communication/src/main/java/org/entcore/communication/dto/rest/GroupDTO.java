package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen(inheritConverter = true)
public class GroupDTO extends IdentifiableDTO {

    private String filter;
    private String profile;
    private String displayName;
    private String groupDisplayName;
    private InternalCommunicationRule internalCommunicationRule;
    private List<IdentifiableDTO> classes = new ArrayList<>();
    private List<IdentifiableDTO> structures = new ArrayList<>();
    private String type;
    private String groupType;
    private String subType;
    private String sortName;
    private Integer nbUsers;

    public GroupDTO() {}

    public GroupDTO(JsonObject json) {
        this();
        GroupDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        GroupDTOConverter.toJson(this, json);
        return json;
    }

    public String getFilter() {
        return filter;
    }

    public GroupDTO setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GroupDTO setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public InternalCommunicationRule getInternalCommunicationRule() {
        return internalCommunicationRule;
    }

    public GroupDTO setInternalCommunicationRule(InternalCommunicationRule internalCommunicationRule) {
        this.internalCommunicationRule = internalCommunicationRule;
        return this;
    }

    public List<IdentifiableDTO> getClasses() {
        return classes;
    }

    public GroupDTO setClasses(List<IdentifiableDTO> classes) {
        this.classes = classes;
        return this;
    }

    public List<IdentifiableDTO> getStructures() {
        return structures;
    }

    public GroupDTO setStructures(List<IdentifiableDTO> structures) {
        this.structures = structures;
        return this;
    }

    public String getSubType() {
        return subType;
    }

    public GroupDTO setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    public String getType() {
        return type;
    }

    public GroupDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getProfile() {
        return profile;
    }

    public GroupDTO setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public String getGroupType() {
        return groupType;
    }

    public GroupDTO setGroupType(String groupType) {
        this.groupType = groupType;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public GroupDTO setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public Integer getNbUsers() {
        return nbUsers;
    }

    public GroupDTO setNbUsers(Integer nbUsers) {
        this.nbUsers = nbUsers;
        return this;
    }

    public String getGroupDisplayName() {
        return groupDisplayName;
    }

    public GroupDTO setGroupDisplayName(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        return this;
    }

    public enum InternalCommunicationRule {
        NONE,
        INCOMING,
        OUTGOING,
        BOTH;
    }
}