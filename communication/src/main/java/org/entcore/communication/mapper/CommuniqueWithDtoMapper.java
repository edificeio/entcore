package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.CommuniqueWithDTO;

public class CommuniqueWithDtoMapper {

    public static CommuniqueWithDTO map(JsonObject json) {
        return json == null ? null : new CommuniqueWithDTO(json);
    }
}