/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.timeline.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TimelineMailerService {

	/**
	 * Sends immediate notification emails for users that are concerned.
	 *
	 * @param request : Request initiating the notification.
	 * @param notificationName : Name of the notification
	 * @param notification : Notification properties
	 * @param templateParameters : Notification parameters
	 * @param recipientIds : Recipients of the notification
	 */
	void sendImmediateMails(final HttpServerRequest request, final String notificationName, final JsonObject notification,
							final JsonObject templateParameters, final JsonArray recipientIds);

	/**
	 * Retrieves stored properties for a single notification.
	 *
	 * @param notificationName : Name of the notification
	 * @param handler : Handles the properties
	 */
	void getNotificationProperties(String notificationName, Handler<Either<String, JsonObject>> handler);

	/**
	 * Translates a key using the usual i18n keys + the timeline folders keys (from all apps).
	 *
	 * @param i18nKeys : Keys to translate
	 * @param domain : Domain of the i18n files
	 * @param language : Language preference of the receiver
	 * @param handler : Handles a JsonArray containing translated Strings.
	 */
	void translateTimeline(JsonArray i18nKeys, String language, String domain, Handler<JsonArray> handler);

	/**
	 * Processes a mustache template using lambdas defined in the Timeline module.
	 *
	 * @param parameters : Template parameters
	 * @param resourceName : Name of the notification (mostly useless)
	 * @param template : Template contents
	 * @param domain : Domain of the i18n files
	 * @param language : Language preference of the receiver
	 * @param handler : Handles the processed template
	 */
	void processTimelineTemplate(JsonObject parameters, String resourceName, String template, String domain,
			String scheme, String language, boolean reader, final Handler<String> handler);

	/**
	 * Send daily notification emails for all users.
	 *
	 * @param dayDelta : When to aggregate, delta from now.
	 * @param handler : Handles the results, emails sent / users KO
	 */
	void sendDailyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler);

	/**
	 * Sends weekly notification emails for all users.
	 *
	 * @param dayDelta : Day from which to aggregate, delta from now.
	 * @param handler : Handles the results, emails sent / users KO
	 */
	void sendWeeklyMails(int dayDelta, final Handler<Either<String, JsonObject>> handler);

	/**
	 * Retrieves default properties for all notifications.
	 * @param handler : Handles the properties
	 */
	void getNotificationsDefaults(final Handler<JsonArray> handler);

}
