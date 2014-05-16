/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.services;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface UserService {

	void createInStructure(String structureId, JsonObject user, Handler<Either<String, JsonObject>> result);

	void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result);

	void update(String id, JsonObject user, Handler<Either<String, JsonObject>> result);

	void sendUserCreatedEmail(HttpServerRequest request, String userId, Handler<Either<String, Boolean>> result);

	void get(String id, Handler<Either<String, JsonObject>> result);

	void list(String structureId, String classId, JsonArray expectedProfiles, Handler<Either<String, JsonArray>> results);

	void listIsolated(String structureId, List<String> profile, Handler<Either<String, JsonArray>> results);

}
