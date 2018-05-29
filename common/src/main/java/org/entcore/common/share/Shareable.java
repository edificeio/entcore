/*
 * Copyright © WebServices pour l'Éducation, 2018
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

package org.entcore.common.share;

import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.http.Renders.renderJson;

public interface Shareable {

	ShareService getShareService();

	TimelineHelper getNotification();

	default void doShare(HttpServerRequest request,  final EventBus eb, String id, UserInfos user, final String notificationName,
						 final JsonObject params, final String resourceNameAttribute) {
		RequestUtils.bodyToJson(request, share -> {
			getShareService().share(user.getUserId(), id, share, r -> {
				if (r.isRight()) {
					JsonArray nta = r.right().getValue().getJsonArray("notify-timeline-array");
					if (nta != null && notificationName != null) {
						notifyShare(request, eb, id, user, nta,	notificationName, params, resourceNameAttribute);
					}
					renderJson(request, r.right().getValue());
				} else {
					JsonObject error = new JsonObject().put("error", r.left().getValue());
					renderJson(request, error, 400);
				}
			});
		});
	}

	default void notifyShare(final HttpServerRequest request, final EventBus eb, final String resource,
							 final UserInfos user, JsonArray sharedArray, final String notificationName,
							 final JsonObject params, final String resourceNameAttribute) {
		final List<String> recipients = new ArrayList<>();
		final AtomicInteger remaining = new AtomicInteger(sharedArray.size());
		for (Object j : sharedArray) {
			JsonObject json = (JsonObject) j;
			String userId = json.getString("userId");
			if (userId != null) {
				recipients.add(userId);
				remaining.getAndDecrement();
			} else {
				String groupId = json.getString("groupId");
				if (groupId != null) {
					UserUtils.findUsersInProfilsGroups(groupId, eb, user.getUserId(), false, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray event) {
							if (event != null) {
								for (Object o : event) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject j = (JsonObject) o;
									String id = j.getString("id");
									recipients.add(id);
								}
							}
							if (remaining.decrementAndGet() < 1) {
								sendNotify(request, resource, user, recipients, notificationName,
										params, resourceNameAttribute);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			sendNotify(request, resource, user, recipients, notificationName, params, resourceNameAttribute);
		}
	}

	default void sendNotify(final HttpServerRequest request, final String resource,
							final UserInfos user, final List<String> recipients, final String notificationName,
							JsonObject params, final String resourceNameAttribute) {
		getNotification().notifyTimeline(request, notificationName, user, recipients, params);
	}

}
