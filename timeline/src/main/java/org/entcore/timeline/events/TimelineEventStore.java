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
			"recipients", "comments", "add-comment", "sub-resource", "event-type", "date");

	List<String> REQUIRED_FIELDS = Arrays.asList("params", "recipients", "type");

	void add(JsonObject event, Handler<JsonObject> result);

	void delete(String resource, Handler<JsonObject> result);

	void get(UserInfos recipient, List<String> types, int offset, int limit,
			JsonObject restrictionFilter, boolean mine, Handler<JsonObject> result);

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
