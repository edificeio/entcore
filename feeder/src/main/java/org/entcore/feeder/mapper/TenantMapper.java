package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.CreateTenantDTO;

public final class TenantMapper {

    private TenantMapper() {}

    public static CreateTenantDTO toCreateTenantDTO(JsonObject body) {
        return new CreateTenantDTO(body.getJsonObject("data", new JsonObject()));
    }
}