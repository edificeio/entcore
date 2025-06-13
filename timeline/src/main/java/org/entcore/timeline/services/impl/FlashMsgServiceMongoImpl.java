/*
 * Copyright © "Open Digital Education", 2018
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

package org.entcore.timeline.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.timeline.services.FlashMsgService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;

@Deprecated
public class FlashMsgServiceMongoImpl extends MongoDbCrudService implements FlashMsgService {

	private final DateFormat mongoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	public FlashMsgServiceMongoImpl(String collection) {
		super(collection);
	}

	@Override
	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		JsonObject now = MongoDb.now();
		data.put("created", now).put("modified", now).put("readCount", 0).put("markedAsRead", new JsonArray());
		mongo.save(collection, data,
				MongoDbResult.validActionResultHandler(handler));
	}

	@Override
	public void update(String id, String structureId, JsonObject data, Handler<Either<String, JsonObject>> handler)
	{
		throw new UnsupportedOperationException("MongoDB Flash msg structureId security not implemented yet");
	}
	@Override
	public void delete(String id, String structureId, Handler<Either<String, JsonObject>> handler)
	{
		throw new UnsupportedOperationException("MongoDB Flash msg structureId security not implemented yet");
	}
	@Override
	public void deleteMultiple(List<String> ids, String structureId, Handler<Either<String, JsonObject>> handler)
	{
		throw new UnsupportedOperationException("MongoDB Flash msg structureId security not implemented yet");
	}

	// Legacy
	public void deleteMultiple(List<String> ids, Handler<Either<String, JsonObject>> handler) {
		Bson q = Filters.in("_id", new JsonArray(ids));
		mongo.delete(collection, MongoQueryBuilder.build(q), validActionResultHandler(handler));
	}

	@Override
	public void list(String domain, Handler<Either<String, JsonArray>> handler) {
		JsonObject match = new JsonObject().put("domain", domain);
		JsonObject sort = new JsonObject().put("modified", -1);
		JsonObject keys = new JsonObject().put("markedAsRead", 0);

		mongo.find(collection, match, sort, keys, validResultsHandler(handler));
	}

	@Override
	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler) {
		String profile = user.getType();
		String now = mongoFormat.format(new Date());

		Bson query = Filters.and(
				Filters.exists("contents."+lang, true),
				Filters.ne("contents."+lang, ""),
				Filters.eq("profiles", profile),
				Filters.ne("markedAsRead", user.getUserId()),
				Filters.lte("startDate", now),
				Filters.gt("endDate", now),
				Filters.eq("domain", domain)
				);

		JsonObject sort = new JsonObject().put("modified", -1);
		JsonObject keys = new JsonObject()
			.put("contents", 1)
			.put("color", 1)
			.put("customColor", 1);

		//Eventually add a limit.

		mongo.find(collection, MongoQueryBuilder.build(query), sort, keys, MongoDbResult.validResultsHandler(handler));
	}

	@Override
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler) {
		String userId = user.getUserId();
		JsonObject queryParam = new JsonObject();
		JsonObject updateParam = new JsonObject();

		queryParam.put("_id", id).put("markedAsRead", new JsonObject().put("$ne", userId));
		updateParam
			.put("$push", new JsonObject()
				.put("markedAsRead", userId))
			.put("$inc", new JsonObject()
				.put("readCount", 1));

		mongo.update(collection, queryParam, updateParam, MongoDbResult.validActionResultHandler(handler));

	}

	@Override
	public void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler) {
		// Not implemented
	}

	@Override
	public void getSubstructuresByMessageId(String messageId, Handler<Either<String, JsonArray>> handler) {
		// Not implemented
	}

	@Override
	public void setSubstructuresByMessageId(String messageId, String structureId, JsonObject subStructures, Handler<Either<String, JsonArray>> handler) {
		// Not implemented
	}

}
