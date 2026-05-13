package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class MergeByKeysDTO {

    private String originalUserId;
    private List<String> mergeKeys;

    public MergeByKeysDTO() {}

    public MergeByKeysDTO(JsonObject json) {
        MergeByKeysDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        MergeByKeysDTOConverter.toJson(this, json);
        return json;
    }

    public String getOriginalUserId() { return originalUserId; }
    public MergeByKeysDTO setOriginalUserId(String originalUserId) { this.originalUserId = originalUserId; return this; }

    public List<String> getMergeKeys() { return mergeKeys; }
    public MergeByKeysDTO setMergeKeys(List<String> mergeKeys) { this.mergeKeys = mergeKeys; return this; }
}