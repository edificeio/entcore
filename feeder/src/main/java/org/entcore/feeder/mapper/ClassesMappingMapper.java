package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.ClassesMappingDTO;

public final class ClassesMappingMapper {

    private ClassesMappingMapper() {}

    public static ClassesMappingDTO toClassesMappingDTO(JsonObject body) {
        // "langage" is a typo in the original protocol that must be preserved for compatibility
        return new ClassesMappingDTO(body).setLanguage(body.getString("langage"));
    }
}