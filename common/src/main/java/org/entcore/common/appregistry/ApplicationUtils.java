package org.entcore.common.appregistry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.appregistry.AppRegistryEvents.APP_REGISTRY_PUBLISH_ADDRESS;
import static org.entcore.common.appregistry.AppRegistryEvents.USER_GROUP_UPDATED;

public final class ApplicationUtils {

	private static final String APP_REGISTRY_ADDRESS = "wse.app.registry.applications";
	private static final String APP_REGISTRY_BUS_ADDRESS = "wse.app.registry.bus";
	private ApplicationUtils() {}

	public static void applicationAllowedUsers(EventBus eb, String application, Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedUsers", application, null, null, handler);
	}

	public static void applicationAllowedUsers(EventBus eb, String application, JsonArray users,
			Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedUsers", application, users, null, handler);
	}

	public static void applicationAllowedUsers(EventBus eb, String application,
			JsonArray users, JsonArray groups, Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedUsers", application, users, groups, handler);
	}

	public static void applicationAllowedProfileGroups(EventBus eb, String application, Handler<JsonArray> handler) {
		applicationInfos(eb, "allowedProfileGroups", application, null, null, handler);
	}

	private static void applicationInfos(EventBus eb, String action, String application,
			JsonArray users, JsonArray groups, final Handler<JsonArray> handler) {
		JsonObject json = new JsonObject();
		json.putString("action", action).putString("application", application);
		if (users != null) {
			json.putArray("users", users);
		}
		if (groups != null) {
			json.putArray("groups", groups);
		}
		eb.send(APP_REGISTRY_ADDRESS, json, new Handler<Message<JsonArray>>() {
			@Override
			public void handle(Message<JsonArray> event) {
				handler.handle(event.body());
			}
		});
	}


	public static void publishModifiedUserGroup(EventBus eb, JsonArray a) {
		eb.publish(APP_REGISTRY_PUBLISH_ADDRESS,
				new JsonObject().putString("type", USER_GROUP_UPDATED)
						.putArray("users", a)
		);
	}

	public static void setDefaultClassRoles(EventBus eb, String classId, Handler<Message<JsonObject>> handler) {
		JsonObject json = new JsonObject();
		json.putString("action", "setDefaultClassRoles").putString("classId", classId);
		eb.send(APP_REGISTRY_BUS_ADDRESS, json, handler);
	}

}
