/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
