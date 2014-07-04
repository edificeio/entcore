/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface CrudService {

	void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void retrieve(String id, Handler<Either<String, JsonObject>> handler);

	void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);

	void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void delete(String id, Handler<Either<String, JsonObject>> handler);

	void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler);

	void list(Handler<Either<String, JsonArray>> handler);

	void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler);

}
