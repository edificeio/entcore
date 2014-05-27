/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http.filter;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

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
			for (String gpId: user.getProfilGroupsIds()) {
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

	protected void executeCountQuery(final HttpServerRequest request, String collection,
			JsonObject query, final int expectedCountResult, final Handler<Boolean> handler) {
		request.pause();
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
