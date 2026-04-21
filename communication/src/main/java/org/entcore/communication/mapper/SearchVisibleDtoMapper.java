package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.SearchVisibleResultDTO;

public class SearchVisibleDtoMapper {

    public static SearchVisibleResultDTO map(JsonObject json) {
        SearchVisibleResultDTO dto = new SearchVisibleResultDTO();

        json.getJsonArray("groups").stream()
                .map(JsonObject.class::cast)
                .map(GroupDtoMapper::map)
                .forEach(dto::addGroup);

        json.getJsonArray("users").stream()
                .map(JsonObject.class::cast)
                .map(UserDtoMapper::map)
                .forEach(dto::addUser);

        return dto;
    }

}
