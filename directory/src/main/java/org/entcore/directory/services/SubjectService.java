package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

public interface SubjectService {
    void getSubjects(String structureId, Handler<Either<String, JsonArray>> results);

}