package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class AddUserFunctionDTO {

    private String userId;
    private String function;
    private List<String> scope;
    private String inherit;
    private Integer transactionId;
    private Boolean commit;
    private Boolean autoSend;

    public AddUserFunctionDTO() {}

    public AddUserFunctionDTO(JsonObject json) {
        AddUserFunctionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        AddUserFunctionDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public AddUserFunctionDTO setUserId(String userId) { this.userId = userId; return this; }

    public String getFunction() { return function; }
    public AddUserFunctionDTO setFunction(String function) { this.function = function; return this; }

    public List<String> getScope() { return scope; }
    public AddUserFunctionDTO setScope(List<String> scope) { this.scope = scope; return this; }

    public String getInherit() { return inherit; }
    public AddUserFunctionDTO setInherit(String inherit) { this.inherit = inherit; return this; }

    public Integer getTransactionId() { return transactionId; }
    public AddUserFunctionDTO setTransactionId(Integer transactionId) { this.transactionId = transactionId; return this; }

    public Boolean getCommit() { return commit; }
    public AddUserFunctionDTO setCommit(Boolean commit) { this.commit = commit; return this; }

    public Boolean getAutoSend() { return autoSend; }
    public AddUserFunctionDTO setAutoSend(Boolean autoSend) { this.autoSend = autoSend; return this; }
}