package org.entcore.timeline.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.timeline.services.FlashMsgService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;

public class FlashMsgServiceMongoImpl extends MongoDbCrudService implements FlashMsgService {

	private final DateFormat mongoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	public FlashMsgServiceMongoImpl(String collection) {
		super(collection);
	}

	@Override
	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		JsonObject now = MongoDb.now();
		data.putObject("created", now).putObject("modified", now).putNumber("readCount", 0).putArray("markedAsRead", new JsonArray());
		mongo.save(collection, data,
				MongoDbResult.validActionResultHandler(handler));
	}

	@Override
	public void deleteMultiple(List<String> ids, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder q = QueryBuilder.start("_id").in(new JsonArray(ids));
		mongo.delete(collection, MongoQueryBuilder.build(q), validActionResultHandler(handler));
	}

	@Override
	public void list(String domain, Handler<Either<String, JsonArray>> handler) {
		JsonObject match = new JsonObject().putString("domain", domain);
		JsonObject sort = new JsonObject().putNumber("modified", -1);
		JsonObject keys = new JsonObject().putNumber("markedAsRead", 0);

		mongo.find(collection, match, sort, keys, validResultsHandler(handler));
	}

	@Override
	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler) {
		String profile = user.getType();
		String now = mongoFormat.format(new Date());

		QueryBuilder query = QueryBuilder.start("contents."+lang).exists(true);
		query.put("contents."+lang).notEquals("");
		query.put("profiles").is(profile);
		query.put("markedAsRead").notEquals(user.getUserId());
		query.put("startDate").lessThanEquals(now);
		query.put("endDate").greaterThan(now);
		query.put("domain").is(domain);

		JsonObject sort = new JsonObject().putNumber("modified", -1);
		JsonObject keys = new JsonObject()
			.putNumber("contents", 1)
			.putNumber("color", 1)
			.putNumber("customColor", 1);

		//Eventually add a limit.

		mongo.find(collection, MongoQueryBuilder.build(query), sort, keys, MongoDbResult.validResultsHandler(handler));
	}

	@Override
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler) {
		String userId = user.getUserId();
		JsonObject queryParam = new JsonObject();
		JsonObject updateParam = new JsonObject();

		queryParam.putString("_id", id).putObject("markedAsRead", new JsonObject().putString("$ne", userId));
		updateParam
			.putObject("$push", new JsonObject()
				.putString("markedAsRead", userId))
			.putObject("$inc", new JsonObject()
				.putNumber("readCount", 1));

		mongo.update(collection, queryParam, updateParam, MongoDbResult.validActionResultHandler(handler));

	}

}
