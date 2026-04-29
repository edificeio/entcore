package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.CreateSubjectDTO;
import org.entcore.feeder.dto.DeleteSubjectDTO;
import org.entcore.feeder.dto.UpdateSubjectDTO;

public final class SubjectMapper {

    private SubjectMapper() {}

    public static CreateSubjectDTO toCreateSubjectDTO(JsonObject body) {
        return new CreateSubjectDTO(body.getJsonObject("subject", new JsonObject()));
    }

    public static UpdateSubjectDTO toUpdateSubjectDTO(JsonObject body) {
        return new UpdateSubjectDTO(body.getJsonObject("subject", new JsonObject()));
    }

    public static DeleteSubjectDTO toDeleteSubjectDTO(JsonObject body) {
        return new DeleteSubjectDTO(body);
    }
}