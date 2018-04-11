/*
 * Copyright © WebServices pour l'Éducation, 2016
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
