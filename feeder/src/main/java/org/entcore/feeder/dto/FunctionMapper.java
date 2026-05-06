package org.entcore.feeder.dto;

import io.vertx.core.json.JsonObject;

public final class FunctionMapper {

    private FunctionMapper() {}

    public static CreateFunctionDTO toCreateFunctionDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        CreateFunctionDTO dto = new CreateFunctionDTO();
        dto.setProfile(body.getString("profile"));
        dto.setExternalId(data.getString("externalId"));
        dto.setName(data.getString("name"));
        return dto;
    }

    public static DeleteFunctionDTO toDeleteFunctionDTO(JsonObject body) {
        DeleteFunctionDTO dto = new DeleteFunctionDTO();
        dto.setFunctionCode(body.getString("functionCode"));
        return dto;
    }

    public static DeleteFunctionGroupDTO toDeleteFunctionGroupDTO(JsonObject body) {
        DeleteFunctionGroupDTO dto = new DeleteFunctionGroupDTO();
        dto.setGroupId(body.getString("groupId"));
        return dto;
    }

    public static JsonObject toFunctionData(CreateFunctionDTO dto) {
        JsonObject data = new JsonObject();
        if (dto.getExternalId() != null) data.put("externalId", dto.getExternalId());
        if (dto.getName() != null) data.put("name", dto.getName());
        return data;
    }
}