package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ImportWithIdDTO;

public final class ImportWithIdMapper {

    private ImportWithIdMapper() {}

    public static ImportWithIdDTO toImportWithIdDTO(JsonObject body) {
        return new ImportWithIdDTO(body);
    }
}