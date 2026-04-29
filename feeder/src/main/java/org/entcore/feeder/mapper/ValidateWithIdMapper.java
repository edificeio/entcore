package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ValidateWithIdDTO;

public final class ValidateWithIdMapper {

    private ValidateWithIdMapper() {}

    public static ValidateWithIdDTO toValidateWithIdDTO(JsonObject body) {
        final ValidateWithIdDTO dto = new ValidateWithIdDTO(body);
        final JsonArray admlStructures = body.getJsonArray("adml-structures");
        if (admlStructures != null) {
            dto.setAdmlStructures(admlStructures.getList());
        }
        return dto;
    }
}