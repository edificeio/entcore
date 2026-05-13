package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateTenantDTO {

    private String externalId;
    private String name;
    private String shortName;
    private String email;
    private String url;
    private String entUrl;
    private List<String> linkRules;

    public CreateTenantDTO() {}

    public CreateTenantDTO(JsonObject json) {
        CreateTenantDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateTenantDTOConverter.toJson(this, json);
        return json;
    }

    public String getExternalId() { return externalId; }
    public CreateTenantDTO setExternalId(String externalId) { this.externalId = externalId; return this; }

    public String getName() { return name; }
    public CreateTenantDTO setName(String name) { this.name = name; return this; }

    public String getShortName() { return shortName; }
    public CreateTenantDTO setShortName(String shortName) { this.shortName = shortName; return this; }

    public String getEmail() { return email; }
    public CreateTenantDTO setEmail(String email) { this.email = email; return this; }

    public String getUrl() { return url; }
    public CreateTenantDTO setUrl(String url) { this.url = url; return this; }

    public String getEntUrl() { return entUrl; }
    public CreateTenantDTO setEntUrl(String entUrl) { this.entUrl = entUrl; return this; }

    public List<String> getLinkRules() { return linkRules; }
    public CreateTenantDTO setLinkRules(List<String> linkRules) { this.linkRules = linkRules; return this; }
}