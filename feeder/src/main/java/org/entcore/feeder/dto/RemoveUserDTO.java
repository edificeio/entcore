package org.entcore.feeder.dto;

public class RemoveUserDTO {

    private String userId;
    private String structureId;
    private String classId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
}