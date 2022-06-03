package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface SubjectService {
    void getSubjects(String structureId, Handler<Either<String, JsonArray>> results);

    void createManual(JsonObject subject, Handler<Either<String, JsonObject>> result);

    void updateManual(JsonObject subject, Handler<Either<String, JsonObject>> result);

    void deleteManual(String subjectId, Handler<Either<String, JsonObject>> notEmptyResponseHandler);
}