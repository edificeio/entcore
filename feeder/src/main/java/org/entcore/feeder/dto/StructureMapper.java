package org.entcore.feeder.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public final class StructureMapper {

    private StructureMapper() {}

    public static CreateStructureDTO toCreateStructureDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        CreateStructureDTO dto = new CreateStructureDTO();
        dto.setName(data.getString("name"));
        dto.setUai(data.getString("UAI"));
        dto.setHasApp(data.getBoolean("hasApp"));
        dto.setTransactionId(body.getInteger("transactionId"));
        dto.setCommit(body.getBoolean("commit", true));
        return dto;
    }

    public static UpdateStructureDTO toUpdateStructureDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        UpdateStructureDTO dto = new UpdateStructureDTO();
        dto.setStructureId(body.getString("structureId"));
        dto.setName(data.getString("name"));
        dto.setUai(data.getString("UAI"));
        dto.setHasApp(data.getBoolean("hasApp"));
        dto.setIgnoreMFA(data.getBoolean("ignoreMFA"));
        dto.setUserLogin(body.getString("userLogin", ""));
        dto.setUserId(body.getString("userId", ""));
        return dto;
    }

    public static JsonObject toStructureProps(CreateStructureDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        putString(props, "UAI", dto.getUai());
        putBoolean(props, "hasApp", dto.getHasApp());
        return props;
    }

    public static JsonObject toStructureProps(UpdateStructureDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        putString(props, "UAI", dto.getUai());
        putBoolean(props, "hasApp", dto.getHasApp());
        putBoolean(props, "ignoreMFA", dto.getIgnoreMFA());
        return props;
    }

    private static void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.put(key, value);
    }

    private static void putBoolean(JsonObject obj, String key, Boolean value) {
        if (value != null) obj.put(key, value);
    }
}