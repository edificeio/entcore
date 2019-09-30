package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ImportService {

    void uploadArchive(final HttpServerRequest request, Handler<Either<String, String>> handler);

    void deleteArchive(String importId);

    void analyzeArchive(UserInfos user, String importId, String locale, JsonObject config, Handler<Either<String, JsonObject>> handler);

}
