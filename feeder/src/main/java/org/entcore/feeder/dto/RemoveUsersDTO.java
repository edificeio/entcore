package org.entcore.feeder.dto;

import java.util.List;

public class RemoveUsersDTO {

    private List<String> userIds;
    private String structureId;
    private List<String> classIds;

    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public List<String> getClassIds() { return classIds; }
    public void setClassIds(List<String> classIds) { this.classIds = classIds; }
}