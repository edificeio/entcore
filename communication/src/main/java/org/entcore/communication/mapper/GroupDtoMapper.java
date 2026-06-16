package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.GroupDTO;

public class GroupDtoMapper {

    public static GroupDTO map(JsonObject json) {
        return json == null ? null : new GroupDTO(json);
    }

}