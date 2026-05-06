package org.entcore.feeder.dto;

import io.vertx.core.json.JsonObject;

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
        RemoveClassDTO dto = new RemoveClassDTO();
        dto.setClassId(body.getString("classId"));
        return dto;
    }

    public static JsonObject toClassProps(CreateClassDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        return props;
    }

    public static JsonObject toClassProps(UpdateClassDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        putString(props, "level", dto.getLevel());
        return props;
    }

    private static void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.put(key, value);
    }
}