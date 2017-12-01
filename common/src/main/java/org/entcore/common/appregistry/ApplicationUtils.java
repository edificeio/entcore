/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.common.appregistry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.appregistry.AppRegistryEvents.APP_REGISTRY_PUBLISH_ADDRESS;
import static org.entcore.common.appregistry.AppRegistryEvents.IMPORT_SUCCEEDED;
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
		json.put("action", action).put("application", application);
		if (users != null) {
			json.put("users", users);
		}
		if (groups != null) {
			json.put("groups", groups);
		}
		eb.send(APP_REGISTRY_ADDRESS, json, new Handler<AsyncResult<Message<JsonArray>>>() {
			@Override
			public void handle(AsyncResult<Message<JsonArray>> event) {
				if (event.succeeded()) {
					handler.handle(event.result().body());
				} else {
					handler.handle(null);
				}
			}
		});
	}


	public static void publishModifiedUserGroup(EventBus eb, JsonArray a) {
		eb.publish(APP_REGISTRY_PUBLISH_ADDRESS,
				new JsonObject().put("type", USER_GROUP_UPDATED)
						.put("users", a)
		);
	}

	public static void sendModifiedUserGroup(EventBus eb, JsonArray a, Handler<AsyncResult<Message<JsonObject>>> res) {
		eb.send(APP_REGISTRY_PUBLISH_ADDRESS,
				new JsonObject().put("type", USER_GROUP_UPDATED)
						.put("users", a), res
		);
	}

	public static void setDefaultClassRoles(EventBus eb, String classId, Handler<AsyncResult<Message<JsonObject>>> handler) {
		JsonObject json = new JsonObject();
		json.put("action", "setDefaultClassRoles").put("classId", classId);
		eb.send(APP_REGISTRY_BUS_ADDRESS, json, handler);
	}

	public static void afterImport(EventBus eb) {
		eb.publish(APP_REGISTRY_PUBLISH_ADDRESS,
				new JsonObject().put("type", IMPORT_SUCCEEDED)
		);
	}

}
