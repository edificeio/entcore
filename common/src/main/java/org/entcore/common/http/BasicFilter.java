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

package org.entcore.common.http;

import fr.wseduc.webutils.collections.Joiner;
import fr.wseduc.webutils.request.filter.AbstractBasicFilter;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BasicFilter extends AbstractBasicFilter {

	@Override
	protected void validateClientScope(String clientId, String secret, final Handler<String> handler) {
		String query =
				"MATCH (n:Application {name: {clientId}, secret: {secret}, grantType: 'Basic'}) " +
				"RETURN n.scope as scope";
		JsonObject params = new JsonObject().put("clientId", clientId).put("secret", secret);
		Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					handler.handle(Joiner.on(" ").join(res.getJsonObject(0).getJsonArray("scope")));
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
