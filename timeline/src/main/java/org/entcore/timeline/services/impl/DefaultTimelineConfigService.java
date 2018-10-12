/*
 * Copyright © "Open Digital Education", 2016
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

 */

package org.entcore.timeline.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.*;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.timeline.services.TimelineConfigService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

import java.util.Map;

public class DefaultTimelineConfigService extends MongoDbCrudService implements TimelineConfigService {

	private Map<String, String> registeredNotifications;

	public DefaultTimelineConfigService(String collection) {
		super(collection);
	}

	@Override
	public void upsert(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		final String key = data.getString("key");
		if(key == null){
			handler.handle(new Either.Left<String, JsonObject>("invalid.key"));
			return;
		}
		mongo.update(collection, new JsonObject().put("key", key), data, true, false, validActionResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		JsonObject sort = new JsonObject().put("modified", -1);
		mongo.find(collection, new JsonObject("{}"), sort, defaultListProjection, validResultsHandler(handler));
	}

	/**
	 * Retrieves stored properties for a single notification.
	 *
	 * @param notificationKey : Name of the notification
	 * @param handler : Handles the properties
	 */
	@Override
	public void getNotificationProperties(final String notificationKey, final Handler<Either<String, JsonObject>> handler) {
		this.list(new Handler<Either<String, JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isLeft()) {
					handler.handle(new Either.Left<String, JsonObject>(
							event.left().getValue()));
					return;
				}
				final String notificationStr = registeredNotifications
						.get(notificationKey.toLowerCase());
				if (notificationStr == null) {
					handler.handle(new Either.Left<String, JsonObject>(
							"invalid.notification.key"));
					return;
				}
				final JsonObject notification = new JsonObject(notificationStr);
				for (Object notifConfigObj : event.right().getValue()) {
					JsonObject notifConfig = (JsonObject) notifConfigObj;
					if (notifConfig.getString("key", "")
							.equals(notificationKey.toLowerCase())) {
						notification.put("defaultFrequency",
								notifConfig.getString("defaultFrequency", ""));
						notification.put("push-notif",
								notifConfig.getBoolean("push-notif", notification.getBoolean("push-notif")));
						notification.put("restriction",
								notifConfig.getString("restriction", ""));
						break;
					}
				}
				handler.handle(
						new Either.Right<String, JsonObject>(notification));
			}
		});
	}

	public void setRegisteredNotifications(Map<String, String> registeredNotifications) {
		this.registeredNotifications = registeredNotifications;
	}

}
