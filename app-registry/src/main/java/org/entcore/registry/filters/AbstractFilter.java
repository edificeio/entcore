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

public abstract class AbstractFilter implements ResourcesProvider {

	private final String label;
	private final Neo4j neo4j = Neo4j.getInstance();

	protected AbstractFilter(String label) {
		this.label = label;
	}

	@Override
	public void authorize(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}
		String roleId = resourceRequest.params().get("id");
		JsonObject params = new JsonObject();
		params.put("structures", new JsonArray(adminLocal.getScope()));
		if (roleId != null && !roleId.trim().isEmpty()) {
			String query =
					"MATCH (r:" + label + " {id : {id}}) " +
					"WHERE HAS(r.structureId) AND r.structureId IN {structures} " +
					"RETURN count(*) > 0 as exists ";
			params.put("id", roleId);
			check(resourceRequest, query, params, handler);
		} else {
			handler.handle(false);
		}
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
