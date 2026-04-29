package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class StructureAttachmentDTO {

    private String structureId;
    private String parentStructureId;
    private Integer transactionId;
    private Boolean commit;
    private Boolean autoSend;

    public StructureAttachmentDTO() {}

    public StructureAttachmentDTO(JsonObject json) {
        StructureAttachmentDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        StructureAttachmentDTOConverter.toJson(this, json);
        return json;
    }

    public String getStructureId() { return structureId; }
    public StructureAttachmentDTO setStructureId(String structureId) { this.structureId = structureId; return this; }

    public String getParentStructureId() { return parentStructureId; }
    public StructureAttachmentDTO setParentStructureId(String parentStructureId) { this.parentStructureId = parentStructureId; return this; }

    public Integer getTransactionId() { return transactionId; }
    public StructureAttachmentDTO setTransactionId(Integer transactionId) { this.transactionId = transactionId; return this; }

    public Boolean getCommit() { return commit; }
    public StructureAttachmentDTO setCommit(Boolean commit) { this.commit = commit; return this; }

    public Boolean getAutoSend() { return autoSend; }
    public StructureAttachmentDTO setAutoSend(Boolean autoSend) { this.autoSend = autoSend; return this; }
}