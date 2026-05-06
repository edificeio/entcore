package org.entcore.feeder.dto;

public class CreateClassDTO {

    private String structureId;
    private String name;
    private Integer transactionId;
    private Boolean commit;

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public Boolean getCommit() { return commit; }
    public void setCommit(Boolean commit) { this.commit = commit; }
}