package org.entcore.infra.services;

import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface EmbedService {

	public void list(Handler<Either<String, JsonArray>> handler);
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void delete(String id, Handler<Either<String, JsonObject>> handler);

}
