package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class GroupDataDTO {

    private String name;
    private String id;
    private String groupDisplayName;
    private String filter;
    private String externalId;
    private String subType;
    private Boolean autolinkTargetAllStructs;
    private List<String> autolinkTargetStructs;
    private List<String> autolinkUsersFromGroups;
    private List<String> autolinkUsersFromPositions;
    private List<String> autolinkUsersFromLevels;
    private Boolean lockDelete;
    private Boolean lockCompose;
    private String createdById;
    private String createdByName;
    private Long createdAt;
    private String modifiedById;
    private String modifiedByName;
    private Long modifiedAt;

    public GroupDataDTO() {}

    public GroupDataDTO(JsonObject json) {
        GroupDataDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        GroupDataDTOConverter.toJson(this, json);
        return json;
    }

    public String getName() { return name; }
    public GroupDataDTO setName(String name) { this.name = name; return this; }

    public String getId() { return id; }
    public GroupDataDTO setId(String id) { this.id = id; return this; }

    public String getGroupDisplayName() { return groupDisplayName; }
    public GroupDataDTO setGroupDisplayName(String groupDisplayName) { this.groupDisplayName = groupDisplayName; return this; }

    public String getFilter() { return filter; }
    public GroupDataDTO setFilter(String filter) { this.filter = filter; return this; }

    public String getExternalId() { return externalId; }
    public GroupDataDTO setExternalId(String externalId) { this.externalId = externalId; return this; }

    public String getSubType() { return subType; }
    public GroupDataDTO setSubType(String subType) { this.subType = subType; return this; }

    public Boolean getAutolinkTargetAllStructs() { return autolinkTargetAllStructs; }
    public GroupDataDTO setAutolinkTargetAllStructs(Boolean autolinkTargetAllStructs) { this.autolinkTargetAllStructs = autolinkTargetAllStructs; return this; }

    public List<String> getAutolinkTargetStructs() { return autolinkTargetStructs; }
    public GroupDataDTO setAutolinkTargetStructs(List<String> autolinkTargetStructs) { this.autolinkTargetStructs = autolinkTargetStructs; return this; }

    public List<String> getAutolinkUsersFromGroups() { return autolinkUsersFromGroups; }
    public GroupDataDTO setAutolinkUsersFromGroups(List<String> autolinkUsersFromGroups) { this.autolinkUsersFromGroups = autolinkUsersFromGroups; return this; }

    public List<String> getAutolinkUsersFromPositions() { return autolinkUsersFromPositions; }
    public GroupDataDTO setAutolinkUsersFromPositions(List<String> autolinkUsersFromPositions) { this.autolinkUsersFromPositions = autolinkUsersFromPositions; return this; }

    public List<String> getAutolinkUsersFromLevels() { return autolinkUsersFromLevels; }
    public GroupDataDTO setAutolinkUsersFromLevels(List<String> autolinkUsersFromLevels) { this.autolinkUsersFromLevels = autolinkUsersFromLevels; return this; }

    public Boolean getLockDelete() { return lockDelete; }
    public GroupDataDTO setLockDelete(Boolean lockDelete) { this.lockDelete = lockDelete; return this; }

    public Boolean getLockCompose() { return lockCompose; }
    public GroupDataDTO setLockCompose(Boolean lockCompose) { this.lockCompose = lockCompose; return this; }

    public String getCreatedById() { return createdById; }
    public GroupDataDTO setCreatedById(String createdById) { this.createdById = createdById; return this; }

    public String getCreatedByName() { return createdByName; }
    public GroupDataDTO setCreatedByName(String createdByName) { this.createdByName = createdByName; return this; }

    public Long getCreatedAt() { return createdAt; }
    public GroupDataDTO setCreatedAt(Long createdAt) { this.createdAt = createdAt; return this; }

    public String getModifiedById() { return modifiedById; }
    public GroupDataDTO setModifiedById(String modifiedById) { this.modifiedById = modifiedById; return this; }

    public String getModifiedByName() { return modifiedByName; }
    public GroupDataDTO setModifiedByName(String modifiedByName) { this.modifiedByName = modifiedByName; return this; }

    public Long getModifiedAt() { return modifiedAt; }
    public GroupDataDTO setModifiedAt(Long modifiedAt) { this.modifiedAt = modifiedAt; return this; }
}
