package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateUserDTO {

    private String profile;
    private String structureId;
    private String classId;
    private List<String> classesNames;
    private String callerId;
    private UserDataDTO data;

    public CreateUserDTO() {}

    public CreateUserDTO(JsonObject json) {
        CreateUserDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateUserDTOConverter.toJson(this, json);
        return json;
    }

    public String getProfile() { return profile; }
    public CreateUserDTO setProfile(String profile) { this.profile = profile; return this; }

    public String getStructureId() { return structureId; }
    public CreateUserDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getClassId() { return classId; }
    public CreateUserDTO setClassId(String classId) { this.classId = classId; return this; }

    public List<String> getClassesNames() { return classesNames; }
    public CreateUserDTO setClassesNames(List<String> classesNames) { this.classesNames = classesNames; return this; }

    public String getCallerId() { return callerId; }
    public CreateUserDTO setCallerId(String callerId) { this.callerId = callerId; return this; }

    public UserDataDTO getData() { return data; }
    public CreateUserDTO setData(UserDataDTO data) { this.data = data; return this; }
}
