package org.entcore.feeder.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public final class StructureMapper {

    private StructureMapper() {}

    public static CreateStructureDTO toCreateStructureDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        CreateStructureDTO dto = new CreateStructureDTO();
        dto.setName(data.getString("name"));
        dto.setExternalId(data.getString("externalId"));
        dto.setFeederName(data.getString("feederName"));
        dto.setSiret(data.getString("SIRET"));
        dto.setSiren(data.getString("SIREN"));
        dto.setJoinKey(toStringList(data.getJsonArray("joinKey")));
        dto.setUai(data.getString("UAI"));
        dto.setType(data.getString("type"));
        dto.setAddress(data.getString("address"));
        dto.setPostbox(data.getString("postbox"));
        dto.setZipCode(data.getString("zipCode"));
        dto.setCity(data.getString("city"));
        dto.setPhone(data.getString("phone"));
        dto.setAccountable(data.getString("accountable"));
        dto.setEmail(data.getString("email"));
        dto.setWebsite(data.getString("website"));
        dto.setContact(data.getString("contact"));
        dto.setMinistry(data.getString("ministry"));
        dto.setContract(data.getString("contract"));
        dto.setAdministrativeAttachment(toStringList(data.getJsonArray("administrativeAttachment")));
        dto.setFunctionalAttachment(toStringList(data.getJsonArray("functionalAttachment")));
        dto.setArea(data.getString("area"));
        dto.setTown(data.getString("town"));
        dto.setDistrict(data.getString("district"));
        dto.setSector(data.getString("sector"));
        dto.setRpi(data.getString("rpi"));
        dto.setAcademy(data.getString("academy"));
        dto.setHasApp(data.getBoolean("hasApp"));
        dto.setGroups(toStringList(data.getJsonArray("groups")));
        dto.setIgnoreMFA(data.getBoolean("ignoreMFA"));
        dto.setTransactionId(body.getInteger("transactionId"));
        dto.setCommit(body.getBoolean("commit", true));
        return dto;
    }

    public static UpdateStructureDTO toUpdateStructureDTO(JsonObject body) {
        JsonObject data = body.getJsonObject("data", new JsonObject());
        UpdateStructureDTO dto = new UpdateStructureDTO();
        dto.setStructureId(body.getString("structureId"));
        dto.setName(data.getString("name"));
        dto.setUai(data.getString("UAI"));
        dto.setHasApp(data.getBoolean("hasApp"));
        dto.setIgnoreMFA(data.getBoolean("ignoreMFA"));
        dto.setUserLogin(body.getString("userLogin", ""));
        dto.setUserId(body.getString("userId", ""));
        return dto;
    }

    public static JsonObject toStructureProps(CreateStructureDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        putString(props, "externalId", dto.getExternalId());
        putString(props, "feederName", dto.getFeederName());
        putString(props, "SIRET", dto.getSiret());
        putString(props, "SIREN", dto.getSiren());
        putList(props, "joinKey", dto.getJoinKey());
        putString(props, "UAI", dto.getUai());
        putString(props, "type", dto.getType());
        putString(props, "address", dto.getAddress());
        putString(props, "postbox", dto.getPostbox());
        putString(props, "zipCode", dto.getZipCode());
        putString(props, "city", dto.getCity());
        putString(props, "phone", dto.getPhone());
        putString(props, "accountable", dto.getAccountable());
        putString(props, "email", dto.getEmail());
        putString(props, "website", dto.getWebsite());
        putString(props, "contact", dto.getContact());
        putString(props, "ministry", dto.getMinistry());
        putString(props, "contract", dto.getContract());
        putList(props, "administrativeAttachment", dto.getAdministrativeAttachment());
        putList(props, "functionalAttachment", dto.getFunctionalAttachment());
        putString(props, "area", dto.getArea());
        putString(props, "town", dto.getTown());
        putString(props, "district", dto.getDistrict());
        putString(props, "sector", dto.getSector());
        putString(props, "rpi", dto.getRpi());
        putString(props, "academy", dto.getAcademy());
        putBoolean(props, "hasApp", dto.getHasApp());
        putList(props, "groups", dto.getGroups());
        putBoolean(props, "ignoreMFA", dto.getIgnoreMFA());
        return props;
    }

    public static JsonObject toStructureProps(UpdateStructureDTO dto) {
        JsonObject props = new JsonObject();
        putString(props, "name", dto.getName());
        putString(props, "UAI", dto.getUai());
        putBoolean(props, "hasApp", dto.getHasApp());
        putBoolean(props, "ignoreMFA", dto.getIgnoreMFA());
        return props;
    }

    private static void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.put(key, value);
    }

    private static void putBoolean(JsonObject obj, String key, Boolean value) {
        if (value != null) obj.put(key, value);
    }

    private static void putList(JsonObject obj, String key, List<String> values) {
        if (values != null) {
            JsonArray arr = new JsonArray();
            values.forEach(arr::add);
            obj.put(key, arr);
        }
    }

    private static List<String> toStringList(JsonArray array) {
        if (array == null) return null;
        return array.stream()
                .filter(o -> o instanceof String)
                .map(o -> (String) o)
                .collect(Collectors.toList());
    }
}