/*
 * Copyright © "Open Digital Education", 2017
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
	 * @param userList : Recipients of the notification
	 * @param notificationProperties : notificationProperties
	 */
	void sendImmediateMails(final HttpServerRequest request, final String notificationName, final JsonObject notification,
							final JsonObject templateParameters, final JsonArray userList, final JsonObject notificationProperties);
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
