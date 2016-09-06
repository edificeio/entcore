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

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.security.JWT;
import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class DefaultOpendIdConnectService implements OpenIdConnectService {

	private final String iss;
	private final JWT jwt;

	public DefaultOpendIdConnectService(String iss, Vertx vertx, String keysPath) {
		this.iss = iss;
		this.jwt = new JWT(vertx, keysPath);
	}

	@Override
	public void generateIdToken(String userId, final String clientId, final AsyncResultHandler<String> handler) {
		String query = "MATCH (u:User {id: {id}}) return u.externalId as sub, u.email as  email, u.displayName as name";
		Neo4j.getInstance().execute(query, new JsonObject().putString("id", userId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					generatePayload(res.<JsonObject>get(0), clientId, handler);
				} else {
					handler.handle(new DefaultAsyncResult<String>(new RuntimeException("invalid.userId")));
				}
			}
		});
	}

	private void generatePayload(JsonObject payload, String clientId, AsyncResultHandler<String> handler) {
		if (payload != null) {
			final long iat = System.currentTimeMillis() / 1000;
			payload.putString("iss", getIss())
					.putString("aud", clientId)
					.putNumber("iat", iat)
					.putNumber("exp", iat + EXPIRATION_TIME);
			try {
				handler.handle(new DefaultAsyncResult<>(jwt.encodeAndSign(payload)));
			} catch (Exception e) {
				handler.handle(new DefaultAsyncResult<String>(e));
			}
		} else {
			handler.handle(new DefaultAsyncResult<String>(new RuntimeException("undefined.payload")));
		}
	}

	@Override
	public String getIss() {
		return iss;
	}

}
