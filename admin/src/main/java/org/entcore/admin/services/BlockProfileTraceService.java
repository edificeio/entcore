package org.entcore.admin.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface BlockProfileTraceService {
    void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler);
}
