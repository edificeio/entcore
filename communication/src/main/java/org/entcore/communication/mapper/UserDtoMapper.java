package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.IdentifiableDTO;
import org.entcore.communication.dto.rest.UserDTO;

import java.util.stream.Collectors;

public class UserDtoMapper {

    public static UserDTO mapDiscoverVisible(JsonObject json) {
        UserDTO dto = new UserDTO();
        dto.setId(json.getString("id"))
                .setName(json.getString("name"));
        dto.setDisplayName(json.getString("displayName"))
                .setGroupDisplayName(json.getString("groupDisplayName"))
                .setProfile(json.getString("profile"))
                .setHasCommunication(json.getBoolean("hasCommunication"));
        if (json.getJsonArray("structures") != null) {
            dto.setStructures(json.getJsonArray("structures").getList());
        }
        return dto;
    }

    public static UserDTO mapDiscoverVisibleGroupUser(JsonObject json) {
        UserDTO dto = new UserDTO();
        dto.setId(json.getString("id"));
        dto.setDisplayName(json.getString("displayName"))
                .setType(json.getString("type"))
                .setLogin(json.getString("login"))
                .setHasCommunication(json.getBoolean("hasCommunication"));
        return dto;
    }

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
