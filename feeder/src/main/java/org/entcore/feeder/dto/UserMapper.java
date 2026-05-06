package org.entcore.feeder.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static CreateUserDTO toCreateUserDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        CreateUserDTO dto = new CreateUserDTO();
        dto.setProfile(body.getString("profile"));
        dto.setStructureId(body.getString("structureId"));
        dto.setClassId(body.getString("classId"));
        JsonArray classesNames = body.getJsonArray("classesNames");
        if (classesNames != null) {
            dto.setClassesNames(classesNames.stream().map(Object::toString).collect(Collectors.toList()));
        }
        dto.setCallerId(body.getString("callerId"));
        dto.setData(toUserDataDTO(data));
        return dto;
    }

    private static UserDataDTO toUserDataDTO(JsonObject data) {
        UserDataDTO dto = new UserDataDTO();
        dto.setFirstName(data.getString("firstName"));
        dto.setLastName(data.getString("lastName"));
        dto.setBirthDate(data.getString("birthDate"));
        dto.setExternalId(data.getString("externalId"));
        dto.setEmail(data.getString("email"));
        dto.setEmailAcademy(data.getString("emailAcademy"));
        dto.setSource(data.getString("source"));
        dto.setProfile(data.getString("profile"));
        dto.setType(data.getString("type"));
        JsonArray profiles = data.getJsonArray("profiles");
        if (profiles != null) {
            dto.setProfiles(profiles.stream().map(Object::toString).collect(Collectors.toList()));
        }
        JsonArray childrenIds = data.getJsonArray("childrenIds");
        if (childrenIds != null) {
            dto.setChildrenIds(childrenIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        JsonArray userPositionIds = data.getJsonArray("userPositionIds");
        if (userPositionIds != null) {
            dto.setUserPositionIds(userPositionIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    public static JsonObject toUserProps(UserDataDTO data) {
        JsonObject props = new JsonObject();
        putString(props, "firstName", data.getFirstName());
        putString(props, "lastName", data.getLastName());
        putString(props, "birthDate", data.getBirthDate());
        putString(props, "externalId", data.getExternalId());
        putString(props, "email", data.getEmail());
        putString(props, "emailAcademy", data.getEmailAcademy());
        putString(props, "source", data.getSource());
        putString(props, "profile", data.getProfile());
        putString(props, "type", data.getType());
        if (data.getProfiles() != null) {
            props.put("profiles", new JsonArray(data.getProfiles()));
        }
        if (data.getChildrenIds() != null) {
            props.put("childrenIds", new JsonArray(data.getChildrenIds()));
        }
        if (data.getUserPositionIds() != null) {
            props.put("userPositionIds", new JsonArray(data.getUserPositionIds()));
        }
        return props;
    }

    public static UpdateUserDTO toUpdateUserDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setUserId(body.getString("userId"));
        dto.setCallerId(body.getString("callerId"));
        dto.setData(toUpdateUserDataDTO(data));
        return dto;
    }

    private static UpdateUserDataDTO toUpdateUserDataDTO(JsonObject data) {
        UpdateUserDataDTO dto = new UpdateUserDataDTO();
        dto.setFirstName(data.getString("firstName"));
        dto.setLastName(data.getString("lastName"));
        dto.setDisplayName(data.getString("displayName"));
        dto.setBirthDate(data.getString("birthDate"));
        dto.setAddress(data.getString("address"));
        dto.setZipCode(data.getString("zipCode"));
        dto.setCity(data.getString("city"));
        dto.setLoginAlias(data.getString("loginAlias"));
        dto.setEmail(data.getString("email"));
        dto.setHomePhone(data.getString("homePhone"));
        dto.setMobile(data.getString("mobile"));
        dto.setChildrenIds(data.getString("childrenIds"));
        JsonArray positionIds = data.getJsonArray("positionIds");
        if (positionIds != null) {
            dto.setPositionIds(positionIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    public static JsonObject toUpdateUserProps(UpdateUserDataDTO data) {
        JsonObject props = new JsonObject();
        putString(props, "firstName", data.getFirstName());
        putString(props, "lastName", data.getLastName());
        putString(props, "displayName", data.getDisplayName());
        putString(props, "birthDate", data.getBirthDate());
        putString(props, "address", data.getAddress());
        putString(props, "zipCode", data.getZipCode());
        putString(props, "city", data.getCity());
        putString(props, "loginAlias", data.getLoginAlias());
        putString(props, "email", data.getEmail());
        putString(props, "homePhone", data.getHomePhone());
        putString(props, "mobile", data.getMobile());
        putString(props, "childrenIds", data.getChildrenIds());
        if (data.getPositionIds() != null) {
            props.put("positionIds", new JsonArray(data.getPositionIds()));
        }
        return props;
    }

    public static AddUserDTO toAddUserDTO(JsonObject body) {
        AddUserDTO dto = new AddUserDTO();
        dto.setUserId(body.getString("userId"));
        dto.setStructureId(body.getString("structureId"));
        dto.setClassId(body.getString("classId"));
        return dto;
    }

    public static AddUsersDTO toAddUsersDTO(JsonObject body) {
        AddUsersDTO dto = new AddUsersDTO();
        JsonArray userIds = body.getJsonArray("userIds");
        if (userIds != null) {
            dto.setUserIds(userIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        dto.setStructureId(body.getString("structureId"));
        dto.setClassId(body.getString("classId"));
        return dto;
    }

    public static UpdateUserLoginDTO toUpdateUserLoginDTO(JsonObject body) {
        UpdateUserLoginDTO dto = new UpdateUserLoginDTO();
        dto.setUserId(body.getString("userId"));
        dto.setLogin(body.getString("login"));
        return dto;
    }

    public static RemoveUserDTO toRemoveUserDTO(JsonObject body) {
        RemoveUserDTO dto = new RemoveUserDTO();
        dto.setUserId(body.getString("userId"));
        dto.setStructureId(body.getString("structureId"));
        dto.setClassId(body.getString("classId"));
        return dto;
    }

    public static RemoveUsersDTO toRemoveUsersDTO(JsonObject body) {
        RemoveUsersDTO dto = new RemoveUsersDTO();
        JsonArray userIds = body.getJsonArray("userIds");
        if (userIds != null) {
            dto.setUserIds(userIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        dto.setStructureId(body.getString("structureId"));
        JsonArray classIds = body.getJsonArray("classIds");
        if (classIds != null) {
            dto.setClassIds(classIds.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    public static DeleteUserDTO toDeleteUserDTO(JsonObject body) {
        DeleteUserDTO dto = new DeleteUserDTO();
        JsonArray users = body.getJsonArray("users");
        if (users != null) {
            dto.setUsers(users.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    public static RestoreUserDTO toRestoreUserDTO(JsonObject body) {
        RestoreUserDTO dto = new RestoreUserDTO();
        JsonArray users = body.getJsonArray("users");
        if (users != null) {
            dto.setUsers(users.stream().map(Object::toString).collect(Collectors.toList()));
        }
        return dto;
    }

    private static void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.put(key, value);
    }
}