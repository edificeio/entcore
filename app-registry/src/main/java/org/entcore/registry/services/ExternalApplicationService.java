package org.entcore.registry.services;

import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface ExternalApplicationService {

	void listExternalApps(String structureId, Handler<Either<String, JsonArray>> handler);
	void listCasConnectors(Handler<Either<String, JsonArray>> handler);
	void deleteExternalApplication(String applicationId, Handler<Either<String, JsonObject>> handler);
	void createExternalApplication(String structureId, JsonObject application, Handler<Either<String, JsonObject>> handler);
	void toggleLock(String structureId, Handler<Either<String, JsonObject>> handler);
	void massAuthorize(String appId, List<String> profiles, Handler<Either<String, JsonObject>> handler);
	void massUnauthorize(String appId, List<String> profiles, Handler<Either<String, JsonObject>> handler);

}
