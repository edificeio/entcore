package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.BusUserDTO;

public class BusUserDtoMapper {

    public static BusUserDTO map(JsonObject json) {
        BusUserDTO dto = new BusUserDTO()
                .setId(json.getString("id"))
                .setName(json.getString("name"))
                .setLogin(json.getString("login"))
                .setUsername(json.getString("username"))
                .setType(json.getString("type"))
                .setProfile(json.getString("profile"))
                .setLastName(json.getString("lastName"))
                .setFirstName(json.getString("firstName"));
        if (json.getJsonArray("profiles") != null) {
            dto.setProfiles(json.getJsonArray("profiles").getList());
        }
        if (json.getJsonArray("positionIds") != null) {
            dto.setPositionIds(json.getJsonArray("positionIds").getList());
        }
        if (json.getJsonArray("positionNames") != null) {
            dto.setPositionNames(json.getJsonArray("positionNames").getList());
        }
        return dto;
    }
}