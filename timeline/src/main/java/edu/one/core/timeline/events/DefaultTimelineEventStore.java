package edu.one.core.timeline.events;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.MongoDb;
import edu.one.core.infra.Server;

import java.util.List;


public class DefaultTimelineEventStore implements TimelineEventStore {

	private static final String TIMELINE_COLLECTION = "timeline";

	private MongoDb mongo;

	public DefaultTimelineEventStore(Vertx vertx, Container container) {
		mongo = new MongoDb(Server.getEventBus(vertx), container.config()
				.getString("mongodb-address", "wse.mongodb.persistor"));
	}

	@Override
	public void add(JsonObject event, final Handler<JsonObject> result) {
		JsonObject doc = validAndGet(event);
		if (doc != null) {
			doc.putObject("date", MongoDb.now());
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
	public void get(String recipient, List<String> types, int offset, int limit, Handler<JsonObject> result) {
		if (recipient != null && !recipient.trim().isEmpty()) {
			JsonObject query = new JsonObject().putString("recipients", recipient);
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
			JsonObject sort = new JsonObject()
			.putObject("$orderby", new JsonObject().putNumber("date", -1));
			JsonObject keys = new JsonObject()
			.putNumber("_id", 0)
			.putNumber("message", 1)
			.putNumber("date", 1)
			.putNumber("sender", 1)
			.putNumber("comments", 1)
			.putNumber("add-comment", 1);
			mongo.find(TIMELINE_COLLECTION, sort.putObject("$query", query), null, keys,
					offset, limit, 100, resultHandler(result));
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

}
