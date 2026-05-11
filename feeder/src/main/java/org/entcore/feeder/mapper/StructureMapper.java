package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.CreateStructureDTO;
import org.entcore.feeder.dto.UpdateStructureDTO;

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
        if (dto.getName() != null) props.put("name", dto.getName());
        if (dto.getUai() != null) props.put("UAI", dto.getUai());
        if (dto.getHasApp() != null) props.put("hasApp", dto.getHasApp());
        return props;
    }

    public static JsonObject toStructureProps(UpdateStructureDTO dto) {
        JsonObject props = new JsonObject();
        if (dto.getName() != null) props.put("name", dto.getName());
        if (dto.getUai() != null) props.put("UAI", dto.getUai());
        if (dto.getHasApp() != null) props.put("hasApp", dto.getHasApp());
        if (dto.getIgnoreMFA() != null) props.put("ignoreMFA", dto.getIgnoreMFA());
        return props;
    }
}