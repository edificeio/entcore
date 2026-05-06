package org.entcore.feeder.dto;

public class CreateGroupDTO {

    private String structureId;
    private String classId;
    private GroupDataDTO group;

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public GroupDataDTO getGroup() { return group; }
    public void setGroup(GroupDataDTO group) { this.group = group; }
}