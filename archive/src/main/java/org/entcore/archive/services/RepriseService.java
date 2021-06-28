package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface RepriseService {

    void launchExportForUsersFromOldPlatform(boolean relativePersonnelFirst);
    void launchImportForUsersFromOldPlatform();
    void imported(String importId, String app, JsonObject rapport);
}
