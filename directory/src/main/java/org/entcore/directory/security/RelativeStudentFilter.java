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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class RelativeStudentFilter implements ResourcesProvider {

	@Override
	public void authorize(final HttpServerRequest request, Binding binding, UserInfos user,
			final Handler<Boolean> handler) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			handler.handle(false);
			return;
		}
		if (functions.containsKey(SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}
		final UserInfos.Function adminLocal = functions.get(DefaultFunctions.ADMIN_LOCAL);
		if (adminLocal == null || adminLocal.getScope() == null) {
			handler.handle(false);
			return;
		}
		final String studentId = request.params().get("studentId");
		final String relativeId = request.params().get("relativeId");
		String query =
				"MATCH (s)<-[:DEPENDS]-(:Group)<-[:IN]-(:User { id : {id}}) " +
				"WHERE (s:Structure OR s:Class) AND s.id IN {scope} " +
				"RETURN count(*) > 0 as exists ";
		JsonArray scope = new JsonArray(adminLocal.getScope());
		StatementsBuilder s = new StatementsBuilder()
				.add(query, new JsonObject()
						.put("id", studentId)
						.put("scope", scope)
				)
				.add(query, new JsonObject()
						.put("id", relativeId)
						.put("scope", scope)
				);
		request.pause();
		Neo4j.getInstance().executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				request.resume();
				JsonArray res = r.body().getJsonArray("results");
				handler.handle(
						"ok".equals(r.body().getString("status")) &&
								res.size() == 2 &&
								res.getJsonArray(0).getJsonObject(0).getBoolean("exists", false) &&
								res.getJsonArray(1).getJsonObject(0).getBoolean("exists", false)
				);
			}
		});
	}

}
