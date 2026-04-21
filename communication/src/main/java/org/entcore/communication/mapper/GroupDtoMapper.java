package org.entcore.communication.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.GroupDTO;

public class GroupDtoMapper {

    public static GroupDTO map(JsonObject json) {
        if(json == null) {
            return null;
        }
        GroupDTO group = new GroupDTO();
        group.setId(json.getString("id"))
                .setName(json.getString("name"));
        group.setFilter(json.getString("filter"))
                .setDisplayName(json.getString("displayName"))
                .setSubType(json.getString("subType"))
                .setType(json.getString("type"))
                .setInternalCommunicationRule(json.getString("internalCommunicationRule") == null ?
                        null : GroupDTO.InternalCommunicationRule.valueOf(json.getString("internalCommunicationRule")));
        if (json.getJsonArray("classes") != null) {
            json.getJsonArray("classes").stream().map(JsonObject.class::cast)
                    .map(IdentifiableDtoMapper::map)
                    .forEach( cl -> group.getClasses().add(cl));
        }
        if (json.getJsonArray("structures") != null) {
            json.getJsonArray("structures").stream().map(JsonObject.class::cast)
                    .map(IdentifiableDtoMapper::map)
                    .forEach( cl -> group.getStructures().add(cl));
        }
        group.setNbUsers(json.getInteger("nbUsers"))
                .setGroupType(json.getString("groupType"))
                .setSortName(json.getString("sortName"))
                .setProfile(json.getString("profile"));
        return group;
    }

}
