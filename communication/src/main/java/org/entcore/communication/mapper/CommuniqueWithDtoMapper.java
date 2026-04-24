package org.entcore.communication.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.CommuniqueWithDTO;
import org.entcore.communication.dto.rest.GroupDTO;

import java.util.stream.Collectors;

public class CommuniqueWithDtoMapper {

    public static CommuniqueWithDTO map(JsonObject json) {
        if (json == null) {
            return null;
        }
        GroupDTO group = GroupDtoMapper.map(json);
        CommuniqueWithDTO dto = new CommuniqueWithDTO();
        dto.setId(group.getId())
                .setName(group.getName());
        dto.setFilter(group.getFilter())
                .setDisplayName(group.getDisplayName())
                .setSubType(group.getSubType())
                .setType(group.getType())
                .setProfile(group.getProfile())
                .setGroupType(group.getGroupType())
                .setSortName(group.getSortName())
                .setNbUsers(group.getNbUsers())
                .setGroupDisplayName(group.getGroupDisplayName())
                .setInternalCommunicationRule(group.getInternalCommunicationRule())
                .setClasses(group.getClasses())
                .setStructures(group.getStructures());

        JsonArray communiqueWith = json.getJsonArray("communiqueWith", new JsonArray());
        dto.setCommuniqueWith(communiqueWith.stream()
                .map(JsonObject.class::cast)
                .map(GroupDtoMapper::map)
                .collect(Collectors.toList()));

        return dto;
    }
}