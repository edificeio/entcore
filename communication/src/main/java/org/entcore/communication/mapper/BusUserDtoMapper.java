package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.bus.BusUserDTO;

public class BusUserDtoMapper {

    public static BusUserDTO map(JsonObject json) {
        return new BusUserDTO(json);
    }
}