package org.entcore.timeline.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.*;

import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.timeline.services.TimelineConfigService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class DefaultTimelineConfigService extends MongoDbCrudService implements TimelineConfigService {

	public DefaultTimelineConfigService(String collection) {
		super(collection);
	}

	@Override
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		super.create(data, user, handler);
	}

	@Override
	public void upsert(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		final String key = data.getString("key");
		if(key == null){
			handler.handle(new Either.Left<String, JsonObject>("invalid.key"));
			return;
		}
		mongo.update(collection, new JsonObject().putString("key", key), data, true, false, validActionResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		JsonObject sort = new JsonObject().putNumber("modified", -1);
		mongo.find(collection, new JsonObject("{}"), sort, defaultListProjection, validResultsHandler(handler));
	}

}
