/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.registry.filters;

import fr.wseduc.webutils.http.Binding;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;

public class LinkRoleGroupFilter implements ResourcesProvider {

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user,
						  final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();

		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}

		bodyToJson(resourceRequest, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final JsonArray roleIds = body.getJsonArray("roleIds");
				final String groupId = body.getString("groupId");
				JsonObject params = new JsonObject();
				params.put("structures", new JsonArray(adminLocal.getScope()));
				if (roleIds != null && groupId != null &&
						roleIds.size() > 0 && !groupId.trim().isEmpty()) {
					String query =
							"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(:Group {id : {groupId}}), (r:Role) " +
							"WHERE s.id IN {structures} AND r.id IN {roles} AND (NOT(HAS(r.structureId)) OR r.structureId IN {structures}) " +
							"RETURN count(distinct r) = {nb} as exists ";
					params.put("groupId", groupId);
					params.put("roles", roleIds);
					params.put("nb", roleIds.size());
					check(resourceRequest, query, params, handler);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void check(final HttpServerRequest request, String query, JsonObject params, final Handler<Boolean> handler) {
		request.pause();
		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonArray r = event.body().getJsonArray("result");
				handler.handle(
						"ok".equals(event.body().getString("status")) &&
								r != null && r.size() == 1 &&
								r.getJsonObject(0).getBoolean("exists", false)
				);
			}
		});
	}

}
