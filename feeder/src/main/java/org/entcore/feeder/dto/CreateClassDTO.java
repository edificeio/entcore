package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class CreateClassDTO {

    private String structureId;
    private String name;
    private Integer transactionId;
    private Boolean commit;

    public CreateClassDTO() {}

    public CreateClassDTO(JsonObject json) {
        CreateClassDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CreateClassDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public CreateClassDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getName() { return name; }
    public CreateClassDTO setName(String name) { this.name = name; return this; }

    public Integer getTransactionId() { return transactionId; }
    public CreateClassDTO setTransactionId(Integer transactionId) { this.transactionId = transactionId; return this; }

    public Boolean getCommit() { return commit; }
    public CreateClassDTO setCommit(Boolean commit) { this.commit = commit; return this; }
}
