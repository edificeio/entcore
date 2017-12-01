/*
 * Copyright © WebServices pour l'Éducation, 2015
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
import org.entcore.common.http.filter.AdmlResourcesProvider;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class AdmlOfUser extends AdmlResourcesProvider {

	@Override
	public void authorizeAdml(final HttpServerRequest resourceRequest, final Binding binding,
			final UserInfos user, final UserInfos.Function adminLocal, final Handler<Boolean> handler) {
		final String userId = resourceRequest.params().get("userId");
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(false);
			return;
		}
		resourceRequest.pause();
		additionnalsChecks(resourceRequest, binding, user, adminLocal, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.FALSE.equals(event)) {
					handler.handle(false);
					return;
				}
				String query =
						"MATCH (u:User {id: {userId}})-[:IN]->(:Group)-[:DEPENDS]->(s:Structure) " +
						"WHERE s.id IN {structures} " +
						"RETURN count(*) > 0 as exists ";

				JsonObject params = new JsonObject()
						.put("structures", new JsonArray(adminLocal.getScope()))
						.put("userId", userId);
				validateQuery(resourceRequest, handler, query, params);
			}
		});

	}

	protected void additionnalsChecks(HttpServerRequest resourceRequest, Binding binding,
			UserInfos user, UserInfos.Function adminLocal, Handler<Boolean> handler) {
		handler.handle(true);
	}

}
