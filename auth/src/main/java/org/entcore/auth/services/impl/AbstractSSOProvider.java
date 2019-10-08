/*
 * Copyright © "Open Digital Education", 2015
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import io.vertx.core.eventbus.EventBus;
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
			"u.login as login, u.email as email, u.mobile as mobile, u.federated, u.blocked as blockedUser, p.blocked as blockedProfile, u.source as source ";

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

	public void generate(EventBus eb, String userId, Handler<Either<String, io.vertx.core.json.JsonArray>> handler) {
		handler.handle(new Either.Left<String, io.vertx.core.json.JsonArray>("Override is required on generate function in AbstractSSOProvider"));
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
					if (attr.equals(attribute.getName()) || attr.equals(attribute.getFriendlyName())) {
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
						if (event.isRight() && (
								event.right().getValue().getBoolean("blockedProfile", false) ||
								event.right().getValue().getBoolean("blockedUser", false)
						)) {
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
					JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
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
