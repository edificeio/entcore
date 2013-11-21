package edu.one.core.timeline.events;

import java.util.Arrays;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface TimelineEventStore {

	List<String> FIELDS = Arrays.asList("resource", "sender", "message", "type",
			"recipients", "comments", "add-comment", "sub-resource", "event-type");

	List<String> REQUIRED_FIELDS = Arrays.asList("resource", "message", "recipients", "type");

	void add(JsonObject event, Handler<JsonObject> result);

	void delete(String resource, Handler<JsonObject> result);

	void get(String recipient, List<String> types, int offset, int limit, Handler<JsonObject> result);

	void deleteSubResource(String resource, Handler<JsonObject> result);

	void listTypes(Handler<JsonArray> result);

}
