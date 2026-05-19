package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.EdtDTO;
import org.entcore.feeder.dto.InitTimetableStructureDTO;
import org.entcore.feeder.dto.UdtDTO;

public final class TimetableMapper {

    private TimetableMapper() {}

    public static InitTimetableStructureDTO toInitTimetableStructureDTO(JsonObject body) {
        return new InitTimetableStructureDTO(body.getJsonObject("conf", new JsonObject()));
    }

    public static EdtDTO toEdtDTO(JsonObject body) {
        // codegen handles path, language, updateGroups, updateTimetable, isManualImport
        // UAI key is non-standard so must be mapped explicitly
        return new EdtDTO(body).setUai(body.getString("UAI"));
    }

    public static UdtDTO toUdtDTO(JsonObject body) {
        return new UdtDTO(body).setUai(body.getString("UAI"));
    }
}