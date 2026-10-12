package org.entcore.communication.dto.bus;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
public class CommunicationBusDTO {

    private String action;
    private List<String> schoolIds;
    private String schoolId;
    private Integer transactionId;
    private Boolean commit;
    private String groupId;
    private String startGroupId;
    private String endGroupId;
    private String direction;

    public CommunicationBusDTO() {}

    public CommunicationBusDTO(JsonObject json) {
        this();
        CommunicationBusDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        CommunicationBusDTOConverter.toJson(this, json);
        return json;
    }

    public String getAction() { return action; }
    public CommunicationBusDTO setAction(String action) { this.action = action; return this; }

    public List<String> getSchoolIds() { return schoolIds; }
    public CommunicationBusDTO setSchoolIds(List<String> schoolIds) { this.schoolIds = schoolIds; return this; }

    public String getSchoolId() { return schoolId; }
    public CommunicationBusDTO setSchoolId(String schoolId) { this.schoolId = schoolId; return this; }

    public Integer getTransactionId() { return transactionId; }
    public CommunicationBusDTO setTransactionId(Integer transactionId) { this.transactionId = transactionId; return this; }

    /** Defaults to {@code true} when absent from the JSON payload. */
    public boolean isCommit() { return commit == null || commit; }
    public CommunicationBusDTO setCommit(boolean commit) { this.commit = commit; return this; }

    public String getGroupId() { return groupId; }
    public CommunicationBusDTO setGroupId(String groupId) { this.groupId = groupId; return this; }

    public String getStartGroupId() { return startGroupId; }
    public CommunicationBusDTO setStartGroupId(String startGroupId) { this.startGroupId = startGroupId; return this; }

    public String getEndGroupId() { return endGroupId; }
    public CommunicationBusDTO setEndGroupId(String endGroupId) { this.endGroupId = endGroupId; return this; }

    public String getDirection() { return direction; }
    public CommunicationBusDTO setDirection(String direction) { this.direction = direction; return this; }
}