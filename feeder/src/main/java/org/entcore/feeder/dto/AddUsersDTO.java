package org.entcore.feeder.dto;

import java.util.List;

public class AddUsersDTO {

    private List<String> userIds;
    private String structureId;
    private String classId;

    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
}