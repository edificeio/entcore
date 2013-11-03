package edu.one.core.common.share.impl;

import com.mongodb.QueryBuilder;
import edu.one.core.infra.Either;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.MongoQueryBuilder;
import edu.one.core.infra.Utils;
import edu.one.core.infra.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class MongoDbShareService extends GenericShareService {

	private final String collection;
	private final MongoDb mongo;

	public MongoDbShareService(EventBus eb, MongoDb mongo, String collection) {
		super(eb);
		this.mongo = mongo;
		this.collection = collection;
	}

	@Override
	public void shareInfos(final String userId, String resourceId, Map<String, SecuredAction> securedActions,
			final Map<String, List<String>> groupedActions, final Handler<Either<String, JsonObject>> handler) {
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid userId."));
			return;
		}
		if (resourceId == null || resourceId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid resourceId."));
			return;
		}
		final JsonArray actions = getResoureActions(securedActions);
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId);
		JsonObject keys = new JsonObject().putNumber("shared", 1);
		mongo.findOne(collection, MongoQueryBuilder.build(query), keys,
			new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray shared = event.body().getObject("result", new JsonObject())
								.getArray("shared", new JsonArray());
						JsonObject s = new JsonObject();
						for (Object o : shared) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject userShared = (JsonObject) o;
							String userOrGroupId = userShared.getString("groupId",
									userShared.getString("userId"));
							JsonArray a = new JsonArray();
							for (String attrName : userShared.getFieldNames()) {
								if ("userId".equals(attrName) || "groupId".equals(attrName)) {
									continue;
								}
								if (groupedActions != null && groupedActions.containsKey(attrName)) {
									for (String action: groupedActions.get(attrName)) {
										a.addString(action.replaceAll("\\.", "-"));
									}
								} else {
									a.addString(attrName);
								}
							}
							s.putArray(userOrGroupId, a);
						}
						getShareInfos(userId, actions, s, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject event) {
								if (event != null && event.size() == 3) {
									handler.handle(new Either.Right<String, JsonObject>(event));
								} else {
									handler.handle(new Either.Left<String, JsonObject>(
											"Error finding shared resource."));
								}
							}
						});
					} else {
						handler.handle(new Either.Left<String, JsonObject>(
								event.body().getString("error", "Error finding shared resource.")));
					}
				}
		});
	}


}
