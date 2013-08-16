package edu.one.core.timeline.events;

import java.util.Arrays;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface TimelineEventStore {

	List<String> FIELDS = Arrays.asList("resource", "sender", "message",
			"recipients", "comments", "add-comment");

	List<String> REQUIRED_FIELDS = Arrays.asList("resource", "message", "recipients");

	void add(JsonObject event, Handler<JsonObject> result);

	void delete(String resource, Handler<JsonObject> result);

	void get(String recipient, int offset, int limit, Handler<JsonObject> result);

}
