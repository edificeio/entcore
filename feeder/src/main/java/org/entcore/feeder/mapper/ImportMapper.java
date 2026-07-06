package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ImportDTO;

public final class ImportMapper {

    private ImportMapper() {}

    public static ImportDTO toImportDTO(JsonObject body) {
        return new ImportDTO(body);
    }
}