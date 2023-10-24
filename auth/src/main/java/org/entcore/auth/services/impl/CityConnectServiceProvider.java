/*
 * Copyright © "Open Digital Education", 2016
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

import org.entcore.auth.services.OpenIdConnectServiceProvider;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.auth.services.HawkAuthorizationService;
import org.entcore.auth.services.HawkAuthorizationServiceImpl;

import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

import java.security.NoSuchAlgorithmException;

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class CityConnectServiceProvider implements OpenIdConnectServiceProvider {

	protected static final Logger log = LoggerFactory.getLogger(CityConnectServiceProvider.class);

	private final String iss;
	private final Neo4j neo4j = Neo4j.getInstance();
	private final HawkAuthorizationService hawkService;
	private final EventBus eb;

	public static final String SCOPE_OPENID = "openid%20profile%20email";

	private static final String SESSION_ADDRESS = "wse.session";
	private static final String LOGOUT_ACTION = "LOGOUT_CIT";


	private static final String QUERY_SUB_CC = "MATCH (u:User {subCC : {sub}}) " + AbstractSSOProvider.RETURN_QUERY;
	private static final String QUERY_PIVOT_CC =
			"MATCH (u:User) WHERE lower(u.email) = lower({email}) AND NOT(HAS(u.subCC)) " +
			"SET u.subCC = {sub}, u.federated = {setFederated} " +
			"WITH u " + AbstractSSOProvider.RETURN_QUERY;
	private static final String QUERY_MAPPING_CC =
			"MATCH (n:User {login:{login}}) " +
			"WHERE NOT(HAS(n.subCC)) " +
			"RETURN n.password as password, n.activationCode as activationCode ";
	private static final String QUERY_SET_MAPPING_CC =
			"MATCH (u:User {login:{login}}) " +
			"WHERE NOT(HAS(u.subCC)) " +
			"SET u.subCC = {sub}, u.federated = {setFederated} " +
			"WITH u " + AbstractSSOProvider.RETURN_QUERY;
	private boolean setFederated = true;

	public CityConnectServiceProvider(String iss, String clientId, String secret, EventBus eb)
	{
		this.iss = iss;
		this.hawkService = new HawkAuthorizationServiceImpl(clientId, secret);
		this.eb = eb;
	}

	@Override
	public void executeFederate(final JsonObject payload, final Handler<Either<String, Object>> handler) {
		if (this.isPayloadValid(payload, this.iss) == true) {
			neo4j.execute(QUERY_SUB_CC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
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
		payload.put("setFederated", setFederated);
		neo4j.execute(QUERY_PIVOT_CC, payload, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
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
		neo4j.execute(QUERY_MAPPING_CC, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					JsonObject res = event.right().getValue();
					try {
                        if (checkPassword(password, res.getString("password"), res.getString("activationCode")) == true) {
                            params.put("setFederated", setFederated);
                            neo4j.execute(QUERY_SET_MAPPING_CC, params.put("sub", payload.getString("sub")),
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
						else
							handler.handle(new Either.Left<String, Object>("auth.error.authenticationFailed"));
					} catch (NoSuchAlgorithmException e) {
						handler.handle(new Either.Left<String, Object>(e.getMessage()));
					}
				} else {
					handler.handle(new Either.Left<String, Object>(event.left().getValue()));
				}
			}
		}));
	}

	@Override
	public void webhook(HttpServerRequest request)
	{
		this.hawkService.authorize(request, new Handler<Boolean>()
		{
			@Override
			public void handle(Boolean isAuthorized)
			{
				if(isAuthorized.booleanValue() == true)
				{
					request.bodyHandler(new Handler<Buffer>()
					{
						@Override
						public void handle(Buffer b)
						{
							JsonObject body = new JsonObject(b.toString("UTF-8"));
							String action = body.getString("action");
							String subject = body.getString("subject");

							if(LOGOUT_ACTION.equals(action))
								webhookLogout(request, subject);
							else
								log.error("Unsupported CC webhook " + action);
						}
					});
				}
				else
					request.response().setStatusCode(401).end();
			}
		});
	}

	public void webhookLogout(HttpServerRequest request, String subject)
	{
		neo4j.execute(QUERY_SUB_CC, new JsonObject().put("sub", subject), validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(final Either<String, JsonObject> event) {
				if (event.isRight() && event.right().getValue().size() > 0)
				{
					String userId = event.right().getValue().getString("id");

					JsonObject sessionMessage = new JsonObject().put("action", "dropByUserId").put("userId", userId);
					eb.request(SESSION_ADDRESS, sessionMessage, new Handler<AsyncResult<Message<JsonObject>>>()
					{
						@Override
						public void handle(AsyncResult<Message<JsonObject>> message)
						{
							if (message.succeeded() == false)
								log.error("Unable to remove session for CC user " + userId + " (subject " + subject + ")");
						}
					});
				}
				else
					log.error("Unable to find CC user (subject " + subject + ")");
				request.response().setStatusCode(200).end();
			}
		}));
	}

	@Override
	public String getScope()
	{
		return SCOPE_OPENID;
	}

	public void setSetFederated(boolean setFederated) {
		this.setFederated = setFederated;
	}
}
