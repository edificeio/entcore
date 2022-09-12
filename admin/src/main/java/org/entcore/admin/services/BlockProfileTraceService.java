package org.entcore.admin.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface BlockProfileTraceService {
    void createTrace(JsonObject data, Handler<Either<String, JsonObject>> handler);
    void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler);
}
