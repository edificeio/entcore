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

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.BCrypt;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class FranceConnectServiceProvider implements OpenIdConnectServiceProvider {

	private final String iss;
	private final Neo4j neo4j = Neo4j.getInstance();
	private static final String QUERY_SUB_FC = "MATCH (u:User {subFC : {sub}}) " + AbstractSSOProvider.RETURN_QUERY;
	private static final String QUERY_PIVOT_FC =
			"MATCH (u:User) WHERE (lower(u.lastName) = lower({family_name}) OR lower(u.lastName) = lower({preferred_username})) " +
			"AND lower({given_name}) CONTAINS lower(u.firstName) AND u.birthDate = {birthdate} AND NOT(HAS(u.subFC)) " +
			"SET u.subFC = {sub}, u.federated = {setFederated} " +
			"WITH u " + AbstractSSOProvider.RETURN_QUERY;
	private static final String QUERY_MAPPING_FC =
			"MATCH (n:User {login:{login}}) " +
			"WHERE NOT(HAS(n.subFC)) " +
			"RETURN n.password as password, n.activationCode as activationCode ";
	private static final String QUERY_SET_MAPPING_FC =
			"MATCH (u:User {login:{login}}) " +
			"WHERE NOT(HAS(u.subFC)) " +
			"SET u.subFC = {sub}, u.federated = {setFederated} " +
			"WITH u " + AbstractSSOProvider.RETURN_QUERY;
	private boolean setFederated = true;

	public FranceConnectServiceProvider(String iss) {
		this.iss = iss;
	}

	@Override
	public void executeFederate(final JsonObject payload, final Handler<Either<String, Object>> handler) {
		if (iss.equals(payload.getString("iss")) && payload.getLong("exp", 0l) > (System.currentTimeMillis() / 1000) &&
				isNotEmpty(payload.getString("sub"))) {
			neo4j.execute(QUERY_SUB_FC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(final Either<String, JsonObject> event) {
					if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
						handler.handle(new Either.Left<String, Object>("blocked.profile"));
					} else if (event.isRight() && event.right().getValue().size() > 0) {
						handler.handle(new Either.Right<String, Object>(event.right().getValue()));
					} else {
						federateWithPivot(payload, handler);
					}
				}
			}));
		} else {
			handler.handle(new Either.Left<String, Object>("invalid.openid.payload"));
		}
	}

	private void federateWithPivot(JsonObject payload, final Handler<Either<String, Object>> handler) {
		if (!payload.containsKey("preferred_username")) {
			payload.put("preferred_username", "");
		}
		payload.put("setFederated", setFederated);
		neo4j.execute(QUERY_PIVOT_FC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(final Either<String, JsonObject> event) {
				if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
					handler.handle(new Either.Left<String, Object>("blocked.profile"));
				} else if (event.isRight() && event.right().getValue().size() > 0) {
					handler.handle(new Either.Right<String, Object>(event.right().getValue()));
				} else {
					handler.handle(new Either.Left<String, Object>(UNRECOGNIZED_USER_IDENTITY));
				}
			}
		}));
	}

	@Override
	public void mappingUser(String login, final String password, final JsonObject payload, final Handler<Either<String, Object>> handler) {
		final JsonObject params = new JsonObject().put("login", login).put("password", password);
		neo4j.execute(QUERY_MAPPING_FC, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					JsonObject res = event.right().getValue();
					boolean success = password.equals(res.getString("activationCode"));
					if (!success && isNotEmpty(res.getString("password"))) {
						try {
							switch (res.getString("password").length()) {
								case 32: // md5
									success = res.getString("password").equals(Md5.hash(password));
									break;
								case 64: // sha-256
									success = res.getString("password").equals(Sha256.hash(password));
									break;
								default: // BCrypt
									success = BCrypt.checkpw(password, res.getString("password"));
							}
						} catch (NoSuchAlgorithmException e) {
							handler.handle(new Either.Left<String, Object>(e.getMessage()));
						}
					}
					if (success) {
						params.put("setFederated", setFederated);
						neo4j.execute(QUERY_SET_MAPPING_FC, params.put("sub", payload.getString("sub")),
								validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
							@Override
							public void handle(final Either<String, JsonObject> event) {
								if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
									handler.handle(new Either.Left<String, Object>("blocked.profile"));
								} else if (event.isRight()) {
									handler.handle(new Either.Right<String, Object>(event.right().getValue()));
								} else {
									handler.handle(new Either.Left<String, Object>("invalid.openid.payload"));
								}
							}
						}));
					}
				} else {
					handler.handle(new Either.Left<String, Object>(event.left().getValue()));
				}
			}
		}));
	}

	public void setSetFederated(boolean setFederated) {
		this.setFederated = setFederated;
	}
}
