package org.entcore.feeder.dto;

import java.util.List;

public class CreateUserDTO {

    private String profile;
    private String structureId;
    private String classId;
    private List<String> classesNames;
    private String callerId;
    private UserDataDTO data;

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public List<String> getClassesNames() { return classesNames; }
    public void setClassesNames(List<String> classesNames) { this.classesNames = classesNames; }

    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }

    public UserDataDTO getData() { return data; }
    public void setData(UserDataDTO data) { this.data = data; }
}