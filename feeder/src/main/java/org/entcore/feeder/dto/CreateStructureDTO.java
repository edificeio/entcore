package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateStructureDTO {

    private String name;
    private String uai;
    private Boolean hasApp;
    private Integer transactionId;
    private Boolean commit;

    public CreateStructureDTO() {}

    public CreateStructureDTO(JsonObject json) {
        CreateStructureDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateStructureDTOConverter.toJson(this, json);
        return json;
    }

    public String getName() { return name; }
    public CreateStructureDTO setName(String name) { this.name = name; return this; }

    public String getUai() { return uai; }
    public CreateStructureDTO setUai(String uai) { this.uai = uai; return this; }

    public Boolean getHasApp() { return hasApp; }
    public CreateStructureDTO setHasApp(Boolean hasApp) { this.hasApp = hasApp; return this; }

    public Integer getTransactionId() { return transactionId; }
    public CreateStructureDTO setTransactionId(Integer transactionId) { this.transactionId = transactionId; return this; }

    public Boolean getCommit() { return commit; }
    public CreateStructureDTO setCommit(Boolean commit) { this.commit = commit; return this; }
}
