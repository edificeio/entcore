package edu.one.core.timeline.controllers;

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.Controller;
import edu.one.core.infra.security.SecuredAction;
import edu.one.core.timeline.events.DefaultTimelineEventStore;
import edu.one.core.timeline.events.TimelineEventStore;

public class TimelineController extends Controller {

	private TimelineEventStore store;

	public TimelineController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		store = new DefaultTimelineEventStore(vertx, container);
	}

	public void view(HttpServerRequest request) {
		renderView(request);
	}

	public void busApi(final Message<JsonObject> message) {
		if (message == null) {
			return;
		}
		JsonObject json = message.body();
		if (json == null) {
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid body."));
			return;
		}

		Handler<JsonObject> handler = new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject event) {
				message.reply(event);
			}
		};

		String action = json.getString("action");
		if (action == null) {
			log.warn("Invalid action.");
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
			return;
		}

		switch (action) {
		case "add":
			store.add(json, handler);
			break;
		case "get":
			store.get(json.getString("recipient"),
					json.getInteger("offset", 0),
					json.getInteger("limit", 25), handler);
			break;
		case "delete":
			store.delete(json.getString("resource"), handler);
			break;
		default:
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
		}
	}

}
