package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ColumnsMappingDTO;

public final class ColumnsMappingMapper {

    private ColumnsMappingMapper() {}

    public static ColumnsMappingDTO toColumnsMappingDTO(JsonObject body) {
        return new ColumnsMappingDTO(body);
    }
}