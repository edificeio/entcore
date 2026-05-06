package org.entcore.feeder.dto;

import java.util.List;

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupDisplayName() { return groupDisplayName; }
    public void setGroupDisplayName(String groupDisplayName) { this.groupDisplayName = groupDisplayName; }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public Boolean getAutolinkTargetAllStructs() { return autolinkTargetAllStructs; }
    public void setAutolinkTargetAllStructs(Boolean autolinkTargetAllStructs) { this.autolinkTargetAllStructs = autolinkTargetAllStructs; }

    public List<String> getAutolinkTargetStructs() { return autolinkTargetStructs; }
    public void setAutolinkTargetStructs(List<String> autolinkTargetStructs) { this.autolinkTargetStructs = autolinkTargetStructs; }

    public List<String> getAutolinkUsersFromGroups() { return autolinkUsersFromGroups; }
    public void setAutolinkUsersFromGroups(List<String> autolinkUsersFromGroups) { this.autolinkUsersFromGroups = autolinkUsersFromGroups; }

    public List<String> getAutolinkUsersFromPositions() { return autolinkUsersFromPositions; }
    public void setAutolinkUsersFromPositions(List<String> autolinkUsersFromPositions) { this.autolinkUsersFromPositions = autolinkUsersFromPositions; }

    public List<String> getAutolinkUsersFromLevels() { return autolinkUsersFromLevels; }
    public void setAutolinkUsersFromLevels(List<String> autolinkUsersFromLevels) { this.autolinkUsersFromLevels = autolinkUsersFromLevels; }

    public Boolean getLockDelete() { return lockDelete; }
    public void setLockDelete(Boolean lockDelete) { this.lockDelete = lockDelete; }

    public Boolean getLockCompose() { return lockCompose; }
    public void setLockCompose(Boolean lockCompose) { this.lockCompose = lockCompose; }

    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getModifiedById() { return modifiedById; }
    public void setModifiedById(String modifiedById) { this.modifiedById = modifiedById; }

    public String getModifiedByName() { return modifiedByName; }
    public void setModifiedByName(String modifiedByName) { this.modifiedByName = modifiedByName; }

    public Long getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Long modifiedAt) { this.modifiedAt = modifiedAt; }
}