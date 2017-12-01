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

package org.entcore.registry.filters;

import java.util.Map;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;

public class WidgetLinkFilter implements ResourcesProvider{

	private final Neo4j neo4j = Neo4j.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {

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

		final String groupId = request.params().get("groupId");
		if(groupId == null || groupId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"MATCH (s:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g:Group {id : {groupId}}) "+
			"WHERE s.id IN {adminScope} " +
			"RETURN count(g) = 1 as exists";
		JsonObject params = new JsonObject()
			.put("groupId", groupId)
			.put("adminScope", new JsonArray(adminLocal.getScope()));

		neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
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
