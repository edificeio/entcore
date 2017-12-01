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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class TeacherOfUser implements ResourcesProvider {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void authorize(final HttpServerRequest resourceRequest, Binding binding, final UserInfos user, final Handler<Boolean> handler) {
		RequestUtils.bodyToJson(resourceRequest, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (event != null) {
					JsonArray userIds = event.getJsonArray("users");
					if (userIds == null || userIds.size() == 0 || userIds.contains(user.getUserId()) ||
							(!"Teacher".equals(user.getType()) && !"Personnel".equals(user.getType()))) {
						handler.handle(false);
						return;
					}
					String query =
							"MATCH (t:User { id : {teacherId}})-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(c:Class) " +
									"WITH c " +
									"MATCH c<-[:DEPENDS]-(og:ProfileGroup)<-[:IN]-(u:User) " +
									"WHERE u.id IN {userIds} " +
									"RETURN count(distinct u) = {size} as exists ";
					JsonObject params = new JsonObject()
							.put("userIds", userIds)
							.put("teacherId", user.getUserId())
							.put("size", userIds.size());
					validateQuery(resourceRequest, handler, query, params);
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
				JsonArray res = r.body().getJsonArray("result");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 1 && (res.getJsonObject(0)).getBoolean("exists", false)
				);
			}
		});
	}

}
