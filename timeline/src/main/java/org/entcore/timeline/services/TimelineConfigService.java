package org.entcore.timeline.services;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface TimelineConfigService{

	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);
	public void upsert(JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void list(Handler<Either<String, JsonArray>> handler);

}