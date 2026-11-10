package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class DeleteFunctionDTO {

    private String functionCode;

    public DeleteFunctionDTO() {}

    public DeleteFunctionDTO(JsonObject json) {
        DeleteFunctionDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        DeleteFunctionDTOConverter.toJson(this, json);
        return json;
    }

    public String getFunctionCode() { return functionCode; }
    public DeleteFunctionDTO setFunctionCode(String functionCode) { this.functionCode = functionCode; return this; }
}
