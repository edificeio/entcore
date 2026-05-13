package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UnmergeByLoginsDTO {

    private String originalUserId;
    private List<String> mergedLogins;

    public UnmergeByLoginsDTO() {}

    public UnmergeByLoginsDTO(JsonObject json) {
        UnmergeByLoginsDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UnmergeByLoginsDTOConverter.toJson(this, json);
        return json;
    }

    public String getOriginalUserId() { return originalUserId; }
    public UnmergeByLoginsDTO setOriginalUserId(String originalUserId) { this.originalUserId = originalUserId; return this; }

    public List<String> getMergedLogins() { return mergedLogins; }
    public UnmergeByLoginsDTO setMergedLogins(List<String> mergedLogins) { this.mergedLogins = mergedLogins; return this; }
}