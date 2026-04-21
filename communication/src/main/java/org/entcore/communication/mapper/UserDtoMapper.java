package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.IdentifiableDTO;
import org.entcore.communication.dto.rest.UserDTO;

import java.util.stream.Collectors;

public class UserDtoMapper {

    public static UserDTO map(JsonObject user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getString("id"));
        dto.setName(user.getString("name"));
        dto.setDisplayName(user.getString("displayName"))
                .setProfile(user.getString("profile"));
        if (user.getJsonArray("subjects") != null) {
            dto.setSubjects( user.getJsonArray("subjects").getList());
        }
        if(user.getJsonArray("positions") != null) {
            dto.setPositions(user.getJsonArray("positions").stream()
                    .map(JsonObject.class::cast)
                    .map( o -> new IdentifiableDTO(o.getString("id"), o.getString("name")))
                    .collect(Collectors.toList()));
        }
        dto.setType(user.getString("type"))
           .setLogin(user.getString("login"));
        return dto;
    }

}
