/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.common.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.*;


public class MongoDbCrudService implements CrudService {

	protected final MongoDb mongo;
	protected final String collection;
	protected final JsonObject defaultRetrieveProjection;
	protected final JsonObject defaultListProjection;

	public MongoDbCrudService(String collection) {
		this(collection, null, null);
	}

	public MongoDbCrudService(String collection,
			JsonObject defaultRetrieveProjection, JsonObject defaultListProjection) {
		this.collection = collection;
		this.defaultRetrieveProjection = defaultRetrieveProjection;
		this.defaultListProjection = defaultListProjection;
		this.mongo = MongoDb.getInstance();
	}

	@Override
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		JsonObject now = MongoDb.now();
		data.putObject("owner", new JsonObject()
				.putString("userId", user.getUserId())
				.putString("displayName", user.getUsername())
		).putObject("created", now).putObject("modified", now);
		mongo.save(collection, data, validActionResultHandler(handler));
	}

	@Override
	public void retrieve(String id, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder builder = QueryBuilder.start("_id").is(id);
		mongo.findOne(collection, MongoQueryBuilder.build(builder),
				defaultRetrieveProjection, validResultHandler(handler));
	}

	@Override
	public void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder builder = QueryBuilder.start("_id").is(id);
		if (user == null) {
			builder.put("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		mongo.findOne(collection, MongoQueryBuilder.build(builder),
				defaultRetrieveProjection,validResultHandler(handler));
	}

	@Override
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler) {
		update(id, data, null, handler);
	}

	@Override
	public void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(id);
		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		for (String attr: data.getFieldNames()) {
			modifier.set(attr, data.getValue(attr));
		}
		modifier.set("modified", MongoDb.now());
		mongo.update(collection, MongoQueryBuilder.build(query),
				modifier.build(), validActionResultHandler(handler));
	}

	@Override
	public void delete(String id, Handler<Either<String, JsonObject>> handler) {
		delete(id, null, handler);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder q = QueryBuilder.start("_id").is(id);
		mongo.delete(collection, MongoQueryBuilder.build(q), validActionResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		JsonObject sort = new JsonObject().putNumber("modified", -1);
		mongo.find(collection, new JsonObject(), sort, defaultListProjection, validResultsHandler(handler));
	}

	@Override
	public void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler) {
		QueryBuilder query;
		if (user != null) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId: user.getProfilGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			switch (filter) {
				case OWNER:
					query = QueryBuilder.start("owner.userId").is(user.getUserId());
					break;
				case OWNER_AND_SHARED:
					query = new QueryBuilder().or(
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
				case SHARED:
					query = QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get());
					break;
				case PROTECTED:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name());
					break;
				case PUBLIC:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
					break;
				default:
					query = new QueryBuilder().or(
							QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
							QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
			}
		} else {
			query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		JsonObject sort = new JsonObject().putNumber("modified", -1);
		mongo.find(collection, MongoQueryBuilder.build(query), sort,
				defaultListProjection, validResultsHandler(handler));
	}

	public void isOwner(String id, UserInfos user, final Handler<Boolean> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner.userId").is(user.getUserId());
		mongo.count(collection, MongoQueryBuilder.build(query), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body();
				handler.handle(res != null && "ok".equals(res.getString("status")) && 1 == res.getInteger("count"));
			}
		});
	}

}
