package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.UpdateUsersOldPlatformDTO;

public final class UpdateUsersOldPlatformMapper {

    private UpdateUsersOldPlatformMapper() {}

    public static UpdateUsersOldPlatformDTO toUpdateUsersOldPlatformDTO(JsonObject body) {
        return new UpdateUsersOldPlatformDTO(body);
    }
}