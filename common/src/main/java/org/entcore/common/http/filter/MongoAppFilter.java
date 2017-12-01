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

package org.entcore.common.http.filter;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MongoAppFilter extends BaseResourceProvider {

	protected String collection;
	protected String resourceIdLabel;
	protected final MongoDb mongo;

	public MongoAppFilter(String collection) {
		this(collection, "id");
	}

	public MongoAppFilter(String collection, String resourceIdLabel) {
		super();
		this.collection = collection;
		this.resourceIdLabel = resourceIdLabel;
		this.mongo = MongoDb.getInstance();
	}

	@Override
	protected String defaultFilter() {
		return "sharedAndOwner";
	}

	public void sharedAndOwner(HttpServerRequest request, String sharedMethod,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get(resourceIdLabel);
		if (id != null && !id.trim().isEmpty()) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId())
					.put(sharedMethod).is(true).get());
			for (String gpId: user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId)
						.put(sharedMethod).is(true).get());
			}
			QueryBuilder query = QueryBuilder.start("_id").is(id).or(
					QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
					QueryBuilder.start("shared").elemMatch(
							new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get()
			);
			executeCountQuery(request, collection, MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	public void ownerOnly(HttpServerRequest request, String sharedMethod,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get(resourceIdLabel);
		if (id != null && !id.trim().isEmpty()) {
			QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner.userId").is(user.getUserId());
			executeCountQuery(request, collection, MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	public static void executeCountQuery(final HttpServerRequest request, String collection,
			JsonObject query, final int expectedCountResult, final Handler<Boolean> handler) {
		request.pause();
		MongoDb mongo = MongoDb.getInstance();
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonObject res = event.body();
				handler.handle(
						res != null &&
								"ok".equals(res.getString("status")) &&
								expectedCountResult == res.getInteger("count")
				);
			}
		});
	}

}
