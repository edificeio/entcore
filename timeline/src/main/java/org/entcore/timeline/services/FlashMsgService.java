package org.entcore.timeline.services;

import java.util.List;

import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface FlashMsgService {

	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler);
	public void delete(String id, Handler<Either<String, JsonObject>> handler);
	public void deleteMultiple(List<String> ids, Handler<Either<String, JsonObject>> handler);
	public void list(String domain, Handler<Either<String, JsonArray>> handler);

	//public void duplicate(String id, Handler<Either<String, JsonObject>> handler);

	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler);
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler);

}
