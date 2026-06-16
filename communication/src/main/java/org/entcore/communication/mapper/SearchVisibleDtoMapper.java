package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.GroupDTO;
import org.entcore.communication.dto.rest.SearchVisibleResultDTO;
import org.entcore.communication.dto.rest.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

public class SearchVisibleDtoMapper {

    public static SearchVisibleResultDTO map(JsonObject json) {
        List<GroupDTO> groups = json.getJsonArray("groups").stream()
                .map(JsonObject.class::cast)
                .map(GroupDtoMapper::map)
                .collect(Collectors.toList());

        List<UserDTO> users = json.getJsonArray("users").stream()
                .map(JsonObject.class::cast)
                .map(UserDtoMapper::map)
                .collect(Collectors.toList());

        return new SearchVisibleResultDTO(groups, users);
    }

}