package edu.one.core.timeline.events;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;


public class DefaultTimelineEventStore implements TimelineEventStore {

	@Override
	public void add(JsonObject event, Handler<JsonObject> result) {
	}

	@Override
	public void delete(String resource, Handler<JsonObject> result) {
	}

	@Override
	public void get(String recipient, int offset, int limit,
			Handler<JsonObject> result) {
	}

}
