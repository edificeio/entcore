package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.AddUserFunctionDTO;
import org.entcore.feeder.dto.CreateFunctionDTO;
import org.entcore.feeder.dto.DeleteFunctionDTO;
import org.entcore.feeder.dto.DeleteFunctionGroupDTO;
import org.entcore.feeder.dto.RemoveUserFunctionDTO;

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
        return new DeleteFunctionDTO(body);
    }

    public static DeleteFunctionGroupDTO toDeleteFunctionGroupDTO(JsonObject body) {
        return new DeleteFunctionGroupDTO(body);
    }

    public static AddUserFunctionDTO toAddUserFunctionDTO(JsonObject body) {
        return new AddUserFunctionDTO(body);
    }

    public static RemoveUserFunctionDTO toRemoveUserFunctionDTO(JsonObject body) {
        return new RemoveUserFunctionDTO(body);
    }

    public static JsonObject toFunctionData(CreateFunctionDTO dto) {
        JsonObject data = dto.toJson();
        data.remove("profile");
        return data;
    }
}