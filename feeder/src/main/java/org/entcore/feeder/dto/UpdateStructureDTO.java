package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateStructureDTO {

    private String structureId;
    private String name;
    private String uai;
    private Boolean hasApp;
    private Boolean ignoreMFA;
    private String userLogin;
    private String userId;

    public UpdateStructureDTO() {}

    public UpdateStructureDTO(JsonObject json) {
        UpdateStructureDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateStructureDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public UpdateStructureDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getName() { return name; }
    public UpdateStructureDTO setName(String name) { this.name = name; return this; }

    public String getUai() { return uai; }
    public UpdateStructureDTO setUai(String uai) { this.uai = uai; return this; }

    public Boolean getHasApp() { return hasApp; }
    public UpdateStructureDTO setHasApp(Boolean hasApp) { this.hasApp = hasApp; return this; }

    public Boolean getIgnoreMFA() { return ignoreMFA; }
    public UpdateStructureDTO setIgnoreMFA(Boolean ignoreMFA) { this.ignoreMFA = ignoreMFA; return this; }

    public String getUserLogin() { return userLogin; }
    public UpdateStructureDTO setUserLogin(String userLogin) { this.userLogin = userLogin; return this; }

    public String getUserId() { return userId; }
    public UpdateStructureDTO setUserId(String userId) { this.userId = userId; return this; }
}