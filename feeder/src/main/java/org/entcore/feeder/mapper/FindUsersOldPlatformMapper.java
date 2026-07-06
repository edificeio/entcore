package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.FindUsersOldPlatformDTO;

public final class FindUsersOldPlatformMapper {

    private FindUsersOldPlatformMapper() {}

    public static FindUsersOldPlatformDTO toFindUsersOldPlatformDTO(JsonObject body) {
        return new FindUsersOldPlatformDTO(body);
    }
}