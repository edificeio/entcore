package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.VerifyResultDTO;

public class VerifyResultDtoMapper {

    public static VerifyResultDTO map(JsonObject json) {
        return new VerifyResultDTO(Boolean.TRUE.equals(json.getBoolean("canCommunicate")));
    }
}