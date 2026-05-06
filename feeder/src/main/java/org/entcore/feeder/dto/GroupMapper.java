package org.entcore.feeder.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.stream.Collectors;

public final class GroupMapper {

    private GroupMapper() {}

    public static CreateGroupDTO toCreateGroupDTO(JsonObject body) {
        CreateGroupDTO dto = new CreateGroupDTO();
        dto.setStructureId(body.getString("structureId"));
        dto.setClassId(body.getString("classId"));
        JsonObject group = body.getJsonObject("group");
        if (group != null) {
            dto.setGroup(toGroupDataDTO(group));
        }
        return dto;
    }

    private static GroupDataDTO toGroupDataDTO(JsonObject group) {
        GroupDataDTO dto = new GroupDataDTO();
        dto.setName(group.getString("name"));
        dto.setId(group.getString("id"));
        dto.setGroupDisplayName(group.getString("groupDisplayName"));
        dto.setFilter(group.getString("filter"));
        dto.setExternalId(group.getString("externalId"));
        dto.setSubType(group.getString("subType"));
        dto.setAutolinkTargetAllStructs(group.getBoolean("autolinkTargetAllStructs"));
        dto.setLockDelete(group.getBoolean("lockDelete"));
        dto.setLockCompose(group.getBoolean("lockCompose"));
        dto.setCreatedById(group.getString("createdById"));
        dto.setCreatedByName(group.getString("createdByName"));
        dto.setCreatedAt(group.getLong("createdAt"));
        dto.setModifiedById(group.getString("modifiedById"));
        dto.setModifiedByName(group.getString("modifiedByName"));
        dto.setModifiedAt(group.getLong("modifiedAt"));
        JsonArray autolinkTargetStructs = group.getJsonArray("autolinkTargetStructs");
        if (autolinkTargetStructs != null) {
            dto.setAutolinkTargetStructs(autolinkTargetStructs.stream().map(Object::toString).collect(Collectors.toList()));
        }
        JsonArray autolinkUsersFromGroups = group.getJsonArray("autolinkUsersFromGroups");
        if (autolinkUsersFromGroups != null) {
            dto.setAutolinkUsersFromGroups(autolinkUsersFromGroups.stream().map(Object::toString).collect(Collectors.toList()));
        }
        JsonArray autolinkUsersFromPositions = group.getJsonArray("autolinkUsersFromPositions");
        if (autolinkUsersFromPositions != null) {
            dto.setAutolinkUsersFromPositions(autolinkUsersFromPositions.stream().map(Object::toString).collect(Collectors.toList()));
        }
        JsonArray autolinkUsersFromLevels = group.getJsonArray("autolinkUsersFromLevels");
        if (autolinkUsersFromLevels != null) {
            dto.setAutolinkUsersFromLevels(autolinkUsersFromLevels.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    public static JsonObject toGroupData(GroupDataDTO dto) {
        JsonObject group = new JsonObject();
        putString(group, "name", dto.getName());
        putString(group, "id", dto.getId());
        putString(group, "groupDisplayName", dto.getGroupDisplayName());
        putString(group, "filter", dto.getFilter());
        putString(group, "externalId", dto.getExternalId());
        putString(group, "subType", dto.getSubType());
        putString(group, "createdById", dto.getCreatedById());
        putString(group, "createdByName", dto.getCreatedByName());
        putString(group, "modifiedById", dto.getModifiedById());
        putString(group, "modifiedByName", dto.getModifiedByName());
        if (dto.getAutolinkTargetAllStructs() != null) group.put("autolinkTargetAllStructs", dto.getAutolinkTargetAllStructs());
        if (dto.getLockDelete() != null) group.put("lockDelete", dto.getLockDelete());
        if (dto.getLockCompose() != null) group.put("lockCompose", dto.getLockCompose());
        if (dto.getCreatedAt() != null) group.put("createdAt", dto.getCreatedAt());
        if (dto.getModifiedAt() != null) group.put("modifiedAt", dto.getModifiedAt());
        if (dto.getAutolinkTargetStructs() != null) group.put("autolinkTargetStructs", new JsonArray(dto.getAutolinkTargetStructs()));
        if (dto.getAutolinkUsersFromGroups() != null) group.put("autolinkUsersFromGroups", new JsonArray(dto.getAutolinkUsersFromGroups()));
        if (dto.getAutolinkUsersFromPositions() != null) group.put("autolinkUsersFromPositions", new JsonArray(dto.getAutolinkUsersFromPositions()));
        if (dto.getAutolinkUsersFromLevels() != null) group.put("autolinkUsersFromLevels", new JsonArray(dto.getAutolinkUsersFromLevels()));
        return group;
    }

    private static void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.put(key, value);
    }
}