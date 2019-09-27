package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface ImportService {

    void uploadArchive(final HttpServerRequest request, Handler<Either<String, String>> handler);

    void analyzeArchive(String importId, String locale, Handler<Either<String, JsonObject>> handler);

}
