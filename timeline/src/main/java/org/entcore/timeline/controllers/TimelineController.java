package org.entcore.timeline.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import fr.wseduc.webutils.Utils;
import fr.wseduc.security.ActionType;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.Controller;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.SecuredAction;
import org.entcore.timeline.events.DefaultTimelineEventStore;
import org.entcore.timeline.events.TimelineEventStore;

public class TimelineController extends Controller {

	private TimelineEventStore store;
	private final ConcurrentMap<String, String> eventsI18n;

	public TimelineController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		store = new DefaultTimelineEventStore(vertx, container);
		eventsI18n = vertx.sharedData().getMap("timelineEventsI18n");
	}

	@SecuredAction("timeline.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("timeline.i18n")
	public void i18n(HttpServerRequest request) {
		String language = Utils.getOrElse(request.headers().get("Accept-Language"), "fr", false);
		String i18n = eventsI18n.get(language.split(",")[0].split("-")[0]);
		if (i18n == null) {
			i18n = eventsI18n.get("fr");
		}
		renderJson(request, new JsonObject("{" + i18n.substring(0, i18n.length() - 1) + "}"));
	}
	
	@SecuredAction("timeline.calendar")
	public void calendar(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("timeline.events")
	public void lastEvents(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					String page = request.params().get("page");
					List<String> types = request.params().getAll("type");
					int offset = 0;
					try {
						offset = 25 * Integer.parseInt(page);
					} catch (NumberFormatException e) {}
					store.get(user.getUserId(), types, offset, 25, new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject res) {
							if (res != null && "ok".equals(res.getString("status"))) {
								renderJson(request, res);
							} else {
								renderError(request, res);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SecuredAction(value = "timeline.auth", type = ActionType.AUTHENTICATED)
	public void listTypes(final HttpServerRequest request) {
		store.listTypes(new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray res) {
				renderJson(request, res);
			}
		});
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
					null,
					json.getInteger("offset", 0),
					json.getInteger("limit", 25), handler);
			break;
		case "delete":
			store.delete(json.getString("resource"), handler);
			break;
		case "deleteSubResource":
			store.deleteSubResource(json.getString("sub-resource"), handler);
		case "list-types":
			store.listTypes(new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray types) {
					message.reply(new JsonObject().putString("status", "ok").putArray("types", types));
				}
			});
			break;
		default:
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "Invalid action."));
		}
	}

}
