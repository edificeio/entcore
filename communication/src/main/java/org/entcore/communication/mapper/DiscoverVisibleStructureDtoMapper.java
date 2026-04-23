package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.DiscoverVisibleStructureDTO;

public class DiscoverVisibleStructureDtoMapper {

    public static DiscoverVisibleStructureDTO map(JsonObject json) {
        return new DiscoverVisibleStructureDTO(
                json.getString("id"),
                json.getString("type"),
                json.getString("label"),
                Boolean.parseBoolean(json.getString("checked"))
        );
    }
}