package org.entcore.infra.services;

import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface EmbedService {

	public void list(Handler<Either<String, JsonArray>> handler);
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void delete(String id, Handler<Either<String, JsonObject>> handler);

}
