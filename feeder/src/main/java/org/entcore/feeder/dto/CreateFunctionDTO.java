package org.entcore.feeder.dto;

public class CreateFunctionDTO {

    private String profile;
    private String externalId;
    private String name;

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}