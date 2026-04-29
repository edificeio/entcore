package org.entcore.feeder.dto;

public class UpdateStructureDTO {

    private String structureId;
    private String name;
    private String uai;
    private Boolean hasApp;
    private Boolean ignoreMFA;
    private String userLogin;
    private String userId;

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUai() { return uai; }
    public void setUai(String uai) { this.uai = uai; }

    public Boolean getHasApp() { return hasApp; }
    public void setHasApp(Boolean hasApp) { this.hasApp = hasApp; }

    public Boolean getIgnoreMFA() { return ignoreMFA; }
    public void setIgnoreMFA(Boolean ignoreMFA) { this.ignoreMFA = ignoreMFA; }

    public String getUserLogin() { return userLogin; }
    public void setUserLogin(String userLogin) { this.userLogin = userLogin; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}