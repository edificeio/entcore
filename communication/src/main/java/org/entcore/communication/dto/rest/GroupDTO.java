package org.entcore.communication.dto.rest;

import com.google.common.collect.Lists;

import java.util.List;

public class GroupDTO extends IdentifiableDTO {

    private String filter;
    private String profile;
    private String displayName;
    private String groupDisplayName;
    private InternalCommunicationRule internalCommunicationRule;
    private List<IdentifiableDTO> classes = Lists.newArrayList();
    private List<IdentifiableDTO> structures = Lists.newArrayList();
    private String type;
    private String groupType;
    private String subType;
    private String sortName;
    private Integer nbUsers;

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
