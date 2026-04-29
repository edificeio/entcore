package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ValidateDTO;

public final class ValidateMapper {

    private ValidateMapper() {}

    public static ValidateDTO toValidateDTO(JsonObject body) {
        final ValidateDTO dto = new ValidateDTO(body);
        final JsonArray admlStructures = body.getJsonArray("adml-structures");
        if (admlStructures != null) {
            dto.setAdmlStructures(admlStructures.getList());
        }
        return dto;
    }
}
