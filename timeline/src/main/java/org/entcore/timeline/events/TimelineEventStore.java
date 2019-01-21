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

package org.entcore.timeline.events;

import java.util.Arrays;
import java.util.List;

import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface TimelineEventStore {

	public enum AdminAction {
		DELETE,
		KEEP
	}

	List<String> FIELDS = Arrays.asList("resource", "sender", "message", "params", "type",
			"recipients", "comments", "add-comment", "sub-resource", "event-type", "date", "pushNotif", "preview");

	List<String> REQUIRED_FIELDS = Arrays.asList("params", "recipients", "type");

	void add(JsonObject event, Handler<JsonObject> result);

	void delete(String resource, Handler<JsonObject> result);

	void get(UserInfos recipient, List<String> types, int offset, int limit,
			JsonObject restrictionFilter, boolean mine, String version, Handler<JsonObject> result);

	void deleteSubResource(String resource, Handler<JsonObject> result);

	void listTypes(Handler<JsonArray> result);

	// User actions
	void delete(String id, String sender, Handler<Either<String, JsonObject>> result);
	void discard(String id, String recipient,  Handler<Either<String, JsonObject>> result);
	void report(String id, UserInfos user, Handler<Either<String, JsonObject>> result);

	// Admin actions
	void listReported(String structure, boolean pending, int offset, int limit, Handler<Either<String, JsonArray>> result);
	void performAdminAction(String id, String structureId, UserInfos user, AdminAction action, Handler<Either<String, JsonObject>> result);
	void deleteReportNotification(String resourceId, Handler<Either<String, JsonObject>> result);
}
