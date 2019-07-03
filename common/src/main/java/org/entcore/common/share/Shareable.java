/*
 * Copyright © "Open Digital Education", 2018
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

	default void doShareSucceed(HttpServerRequest request, String id, UserInfos user,JsonObject sharePayload, JsonObject result, boolean sendNotification){
		renderJson(request, result);
	}

	default void doShare(HttpServerRequest request,  final EventBus eb, String id, UserInfos user, final String notificationName,
						 final JsonObject params, final String resourceNameAttribute) {
		RequestUtils.bodyToJson(request, share -> {
			getShareService().share(user.getUserId(), id, share, r -> {
				if (r.isRight()) {
					JsonArray nta = r.right().getValue().getJsonArray("notify-timeline-array");
					boolean sendNotification = false;
					if (nta != null && notificationName != null) {
						sendNotification = true;
						notifyShare(request, eb, id, user, nta,	notificationName, params, resourceNameAttribute);
					}
					doShareSucceed(request, id, user, share, r.right().getValue(), sendNotification);
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
