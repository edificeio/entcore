package org.entcore.timeline.events;

import fr.wseduc.mongodb.MongoDb;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.Server;

import java.util.List;


public class DefaultTimelineEventStore implements TimelineEventStore {

	private static final String TIMELINE_COLLECTION = "timeline";

	private MongoDb mongo;

	public DefaultTimelineEventStore(Vertx vertx, Container container) {
		mongo = MongoDb.getInstance();
		mongo.init(Server.getEventBus(vertx), container.config()
				.getString("mongodb-address", "wse.mongodb.persistor"));
	}

	@Override
	public void add(JsonObject event, final Handler<JsonObject> result) {
		JsonObject doc = validAndGet(event);
		if (doc != null) {
			if (!doc.containsField("date")) {
				doc.putObject("date", MongoDb.now());
			}
			mongo.save(TIMELINE_COLLECTION, doc, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void delete(String resource, Handler<JsonObject> result) {
		if (resource != null && !resource.trim().isEmpty()) {
			JsonObject query = new JsonObject()
			.putString("resource", resource);
			mongo.delete(TIMELINE_COLLECTION, query, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void get(final UserInfos user, List<String> types, int offset, int limit,
			final Handler<JsonObject> result) {
		final String recipient = user.getUserId();
		final String externalId = user.getExternalId();
		if (recipient != null && !recipient.trim().isEmpty()) {
			final JsonObject query = new JsonObject()
					.putObject("date", new JsonObject().putObject("$lt", MongoDb.now()));
			if (externalId == null || externalId.trim().isEmpty()) {
				query.putString("recipients.userId", recipient);
			} else {
				query.putObject("recipients.userId", new JsonObject()
						.putArray("$in", new JsonArray().add(recipient).add(externalId)));
			}
			if (types != null && !types.isEmpty()) {
				if (types.size() == 1) {
					query.putString("type", types.get(0));
				} else {
					JsonArray typesFilter = new JsonArray();
					for (String t: types) {
						typesFilter.addObject(new JsonObject().putString("type", t));
					}
					query.putArray("$or", typesFilter);
				}
			}
			JsonObject sort = new JsonObject().putNumber("date", -1);
			JsonObject keys = new JsonObject()
			.putNumber("message", 1)
			.putNumber("date", 1)
			.putNumber("sender", 1)
			.putNumber("recipients.$", 1)
			.putNumber("comments", 1)
			.putNumber("event-type", 1)
			.putNumber("resource", 1)
			.putNumber("sub-resource", 1)
			.putNumber("add-comment", 1);
			mongo.find(TIMELINE_COLLECTION, query, sort, keys,
					offset, limit, 100, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					result.handle(message.body());
					markEventsAsRead(message, recipient);
				}
			});
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void deleteSubResource(String resource, Handler<JsonObject> result) {
		if (resource != null && !resource.trim().isEmpty()) {
			JsonObject query = new JsonObject()
					.putString("sub-resource", resource);
			mongo.delete(TIMELINE_COLLECTION, query, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void listTypes(final Handler<JsonArray> result) {
		mongo.distinct(TIMELINE_COLLECTION, "type", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					result.handle(event.body().getArray("values", new JsonArray()));
				} else {
					result.handle(new JsonArray());
				}
			}
		});
	}

	private JsonObject validAndGet(JsonObject json) {
		if (json != null) {
			JsonObject e = json.copy();
			for (String attr: json.getFieldNames()) {
				if (!FIELDS.contains(attr) || e.getValue(attr) == null) {
					e.removeField(attr);
				}
			}
			if (e.toMap().keySet().containsAll(REQUIRED_FIELDS)) {
				return e;
			}
		}
		return null;
	}

	private JsonObject invalidArguments() {
		return new JsonObject().putString("status", "error")
				.putString("message", "Invalid arguments.");
	}


	private Handler<Message<JsonObject>> resultHandler(final Handler<JsonObject> result) {
		return new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				result.handle(message.body());
			}
		};
	}

	private void markEventsAsRead(Message<JsonObject> message, String recipient) {
		JsonArray events = message.body().getArray("results");
		if (events != null && "ok".equals(message.body().getString("status"))) {
			JsonArray ids = new JsonArray();
			for (Object o : events) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject json = (JsonObject) o;
				ids.addString(json.getString("_id"));
			}
			JsonObject q = new JsonObject()
					.putObject("_id", new JsonObject().putArray("$in", ids))
					.putObject("recipients", new JsonObject().putObject("$elemMatch",
							new JsonObject().putString("userId", recipient).putNumber("unread", 1)
					));
			mongo.update(TIMELINE_COLLECTION, q, new JsonObject().putObject("$set",
					new JsonObject().putNumber("recipients.$.unread", 0)), false, true);
		}
	}

}
