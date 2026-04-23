package org.entcore.communication.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.communication.dto.rest.ContactRefDTO;
import org.entcore.communication.dto.rest.SearchVisibleContactDTO;

import java.util.List;
import java.util.stream.Collectors;

public class SearchVisibleContactDtoMapper {

    public static SearchVisibleContactDTO map(JsonObject json) {
        return new SearchVisibleContactDTO()
                .setId(json.getString("id"))
                .setDisplayName(json.getString("displayName"))
                .setType(json.getString("type"))
                .setUsedIn(mapStringArray(json.getJsonArray("usedIn")))
                .setProfile(json.getString("profile"))
                .setGroupType(json.getString("groupType"))
                .setNbUsers(json.getInteger("nbUsers"))
                .setStructureName(json.getString("structureName"))
                .setChildren(mapContactRefs(json.getJsonArray("children")))
                .setRelatives(mapContactRefs(json.getJsonArray("relatives")));
    }

    private static List<String> mapStringArray(JsonArray array) {
        if (array == null) return null;
        return array.stream().map(Object::toString).collect(Collectors.toList());
    }

    private static List<ContactRefDTO> mapContactRefs(JsonArray array) {
        if (array == null) return null;
        return array.stream()
                .map(JsonObject.class::cast)
                .map(o -> new ContactRefDTO()
                        .setId(o.getString("id"))
                        .setDisplayName(o.getString("displayName")))
                .collect(Collectors.toList());
    }
}