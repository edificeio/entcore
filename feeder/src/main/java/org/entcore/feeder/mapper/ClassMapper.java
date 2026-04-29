package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.CreateClassDTO;
import org.entcore.feeder.dto.RemoveClassDTO;
import org.entcore.feeder.dto.UpdateClassDTO;

public final class ClassMapper {

    private ClassMapper() {}

    public static CreateClassDTO toCreateClassDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        CreateClassDTO dto = new CreateClassDTO();
        dto.setStructureId(body.getString("structureId"));
        dto.setName(data.getString("name"));
        dto.setTransactionId(body.getInteger("transactionId"));
        dto.setCommit(body.getBoolean("commit", true));
        return dto;
    }

    public static UpdateClassDTO toUpdateClassDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        UpdateClassDTO dto = new UpdateClassDTO();
        dto.setClassId(body.getString("classId"));
        dto.setName(data.getString("name"));
        dto.setLevel(data.getString("level"));
        return dto;
    }

    public static RemoveClassDTO toRemoveClassDTO(JsonObject body) {
        return new RemoveClassDTO(body);
    }

    public static JsonObject toClassProps(CreateClassDTO dto) {
        JsonObject props = dto.toJson();
        props.remove("structureId");
        props.remove("transactionId");
        props.remove("commit");
        return props;
    }

    public static JsonObject toClassProps(UpdateClassDTO dto) {
        JsonObject props = dto.toJson();
        props.remove("classId");
        return props;
    }
}