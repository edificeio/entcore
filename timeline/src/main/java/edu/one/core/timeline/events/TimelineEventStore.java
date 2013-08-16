package edu.one.core.timeline.events;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface TimelineEventStore {

	void add(JsonObject event, Handler<JsonObject> result);

	void delete(String resource, Handler<JsonObject> result);

	void get(String recipient, int offset, int limit, Handler<JsonObject> result);

}
