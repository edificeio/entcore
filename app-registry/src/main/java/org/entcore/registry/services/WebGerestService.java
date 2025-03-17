package org.entcore.registry.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface WebGerestService {
    void getMenu(String UAI, JsonObject config, Handler<Either<String, JsonObject>> eitherHandler);
}
