package edu.one.core.common.appregistry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public final class ApplicationUtils {

	private static final String APP_REGISTRY_ADDRESS = "wse.app.registry.applications";
	private ApplicationUtils() {}

	public static void applicationAllowedUsers(EventBus eb, String application, Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedUsers", application, handler);
	}

	public static void applicationAllowedProfileGroups(EventBus eb, String application, Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedProfileGroups", application, handler);
	}

	private static void applicationInfos(EventBus eb, String action, String application,
			final Handler<JsonArray> handler) {
		JsonObject json = new JsonObject();
		json.putString("action", action).putString("application", application);
		eb.send(APP_REGISTRY_ADDRESS, json, new Handler<Message<JsonArray>>() {
			@Override
			public void handle(Message<JsonArray> event) {
				handler.handle(event.body());
			}
		});
	}

}
