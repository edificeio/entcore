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

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.xml.XMLObject;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractSSOProvider implements SamlServiceProvider {

	static final String RETURN_QUERY = "OPTIONAL MATCH (p:Profile) " +
			"WHERE HAS(u.profiles) AND p.name = head(u.profiles) " +
			"RETURN DISTINCT u.id as id, u.activationCode as activationCode, " +
			"u.login as login, u.email as email, u.mobile as mobile, u.federated, p.blocked as blockedProfile ";

	protected boolean validConditions(Assertion assertion, Handler<Either<String, Object>> handler) {
		if (Utils.validationParamsNull(handler, "invalid.assertion", assertion)) return false;

		Conditions conditions = assertion.getConditions();
		if (conditions.getNotBefore() == null || !conditions.getNotBefore().isBeforeNow() ||
				conditions.getNotOnOrAfter() == null || !conditions.getNotOnOrAfter().isAfterNow()) {
			handler.handle(new Either.Left<String, Object>("invalid.conditions"));
			return false;
		}
		return true;
	}

	protected String getAttribute(Assertion assertion, String attr) {
		if (assertion.getAttributeStatements() != null) {
			for (AttributeStatement statement : assertion.getAttributeStatements()) {
				for (Attribute attribute : statement.getAttributes()) {
					if (attr.equals(attribute.getName())) {
						for (XMLObject o : attribute.getAttributeValues()) {
							if (o.getDOM() != null) {
								return o.getDOM().getTextContent();
							}
						}
					}
				}
			}
		}
		return null;
	}

	protected List<String> getAttributes(Assertion assertion, String attr) {
		List<String> attributes = new ArrayList<>();
		if (assertion.getAttributeStatements() != null) {
			for (AttributeStatement statement : assertion.getAttributeStatements()) {
				for (Attribute attribute : statement.getAttributes()) {
					if (attr.equals(attribute.getName())) {
						for (XMLObject o : attribute.getAttributeValues()) {
							if (o.getDOM() != null) {
								attributes.add(o.getDOM().getTextContent());
							}
						}
					}
				}
			}
		}
		return attributes;
	}

	protected void executeQuery(String query, final JsonObject params, final Handler<Either<String, Object>> handler) {
		executeQuery(query, params, null, handler);
	}

	protected void executeQuery(String query, final JsonObject params, final Assertion assertion,
			final Handler<Either<String, Object>> handler) {
		executeFederateQuery(query, params, assertion, Neo4j.getInstance(), handler);
	}

	static void executeFederateQuery(String query, JsonObject params, final Assertion assertion,
									 Neo4j neo4j, final Handler<Either<String, Object>> handler) {
		executeFederateQuery(query, params, assertion, true, neo4j, handler);
	}

	static void executeFederateQuery(String query, JsonObject params, final Assertion assertion, final boolean setFederated,
			Neo4j neo4j, final Handler<Either<String, Object>> handler) {
		query += RETURN_QUERY;
		neo4j.execute(query, params, Neo4jResult.validUniqueResultHandler(
				new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(final Either<String, JsonObject> event) {
						if (event.isRight() && event.right().getValue().getBoolean("blockedProfile", false)) {
							handler.handle(new Either.Left<String, Object>("blocked.profile"));
						} else if (setFederated &&  event.isRight() && event.right().getValue().getBoolean("federated") == null &&
								event.right().getValue().getString("id") != null) {
							String query = "MATCH (u:User {id: {id}}) SET u.federated = true ";
							JsonObject params = new JsonObject().put("id", event.right().getValue().getString("id"));
							if (assertion != null && assertion.getIssuer() != null &&
									assertion.getIssuer().getValue() != null && !assertion.getIssuer().getValue().trim().isEmpty()) {
								query += ", u.federatedIDP = {idp} ";
								params.put("idp", assertion.getIssuer().getValue());
							}
							Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event2) {
									handler.handle(new Either.Right<String, Object>(event.right().getValue()));
								}
							});
						} else if (event.isRight()) {
							handler.handle(new Either.Right<String, Object>(event.right().getValue()));
						} else {
							handler.handle(new Either.Left<String, Object>(event.left().getValue()));
						}
					}
				}));
	}

	protected void executeMultiVectorQuery(String query, JsonObject params, final Assertion assertion,
			final Handler<Either<String, Object>> handler) {
		query += (RETURN_QUERY + ", s.name as structureName");
		Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(final Either<String, JsonArray> event) {
				if (event.isRight()) {
					JsonArray ids = new JsonArray();
					final Set<String> userIds = new HashSet<>();
					final JsonArray users = event.right().getValue();
					for (Object o: users) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						if (j.getBoolean("blockedProfile", false)) {
							handler.handle(new Either.Left<String, Object>("blocked.profile"));
							return;
						}
						userIds.add(j.getString("id"));
						if (Utils.isNotEmpty(j.getString("id")) && !j.getBoolean("federated", false)) {
							ids.add(j.getString("id"));
						}
					}
					if (ids.size() > 0) {
						String query = "MATCH (u:User) WHERE u.id IN {ids} SET u.federated = true ";
						JsonObject params = new JsonObject().put("ids", ids);
						if (assertion != null && assertion.getIssuer() != null &&
								assertion.getIssuer().getValue() != null && !assertion.getIssuer().getValue().trim().isEmpty()) {
							query += ", u.federatedIDP = {idp} ";
							params.put("idp", assertion.getIssuer().getValue());
						}
						Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event2) {
								if (userIds.size() == 1) {
									handler.handle(new Either.Right<String, Object>(users.getJsonObject(0)));
								} else {
									handler.handle(new Either.Right<String, Object>(users));
								}
							}
						});
					} else {
						if (userIds.size() == 1) {
							handler.handle(new Either.Right<String, Object>(users.getJsonObject(0)));
						} else {
							handler.handle(new Either.Right<String, Object>(users));
						}
					}
				} else {
					handler.handle(new Either.Left<String, Object>(event.left().getValue()));
				}
			}
		}));
	}

}
