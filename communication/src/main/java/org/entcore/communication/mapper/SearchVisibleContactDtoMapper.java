package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.SearchVisibleContactDTO;

public class SearchVisibleContactDtoMapper {

    public static SearchVisibleContactDTO map(JsonObject json) {
        return new SearchVisibleContactDTO(json);
    }
}