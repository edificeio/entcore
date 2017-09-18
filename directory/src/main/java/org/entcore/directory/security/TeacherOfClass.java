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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;


public class TeacherOfClass implements ResourcesProvider {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {
		final String classId = request.params().get("classId");
		if (classId == null || classId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		Set<String> ids = DirectoryResourcesProvider.getIds(user);
		String query =
				"MATCH (c:Class {id : {classId}})-[:BELONGS]->s2 " +
						"WHERE s2.id IN {ids} " +
						"RETURN count(*) > 0 as exists";
		JsonObject params = new JsonObject()
				.putString("classId", classId)
				.putString("userId", request.params().get("userId"))
				.putArray("ids", new JsonArray(ids.toArray()));
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				if ("ok".equals(r.body().getString("status")) &&
						res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)) {
					handler.handle(true);
				} else if ("Teacher".equals(user.getType()) || "Personnel".equals(user.getType())) {
					String query =
							"MATCH (c:`Class` { id : {classId}})<-[:DEPENDS]-(pg:ProfileGroup)" +
									"<-[:IN]-(t:`User` { id : {teacherId}}) " +
									"RETURN count(*) > 0 as exists ";
					JsonObject params = new JsonObject()
							.putString("classId", classId)
							.putString("teacherId", user.getUserId());
					validateQuery(request, handler, query, params);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void validateQuery(final HttpServerRequest request, final Handler<Boolean> handler, String query, JsonObject params) {
		request.pause();
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getArray("result");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 1 && ((JsonObject) res.get(0)).getBoolean("exists", false)
				);
			}
		});
	}

}
