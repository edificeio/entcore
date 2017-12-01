/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.auth.oauth;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import jp.eisbahn.oauth2.server.async.Handler;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.models.AccessToken;
import jp.eisbahn.oauth2.server.models.AuthInfo;
import jp.eisbahn.oauth2.server.models.Request;

import org.entcore.auth.services.OpenIdConnectService;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.wseduc.webutils.security.BCrypt;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.getOrElse;

public class OAuthDataHandler extends DataHandler {
	private final Neo4j neo;
	private final MongoDb mongo;
	private final OpenIdConnectService openIdConnectService;
	private final boolean checkFederatedLogin;
	private static final String AUTH_INFO_COLLECTION = "authorizations";
	private static final String ACCESS_TOKEN_COLLECTION = "tokens";
	private static final int CODE_EXPIRES = 600000; // 10 min
	private static final Logger log = LoggerFactory.getLogger(OAuthDataHandler.class);

	public OAuthDataHandler(Request request, Neo4j neo, MongoDb mongo, OpenIdConnectService openIdConnectService,
			boolean checkFederatedLogin) {
		super(request);
		this.neo = neo;
		this.mongo = mongo;
		this.openIdConnectService = openIdConnectService;
		this.checkFederatedLogin = checkFederatedLogin;
	}

	@Override
	public void validateClient(String clientId, String clientSecret,
			String grantType, final Handler<Boolean> handler) {
		String query =
				"MATCH (n:Application) " +
				"WHERE n.name = {clientId} " +
				"AND n.secret = {secret} AND n.grantType = {grantType} " +
				"RETURN count(n) as nb";
		Map<String, Object> params = new HashMap<>();
		params.put("clientId", clientId);
		params.put("secret", clientSecret);
		params.put("grantType", grantType);
		neo.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				JsonArray a = res.body().getJsonArray("result");
				if ("ok".equals(res.body().getString("status")) && a != null && a.size() == 1) {
					JsonObject r = a.getJsonObject(0);
					handler.handle(r != null && r.getInteger("nb") == 1);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	@Override
	public void getUserId(final String username, final String password, final Handler<String> handler) {
		if (username != null && password != null &&
				!username.trim().isEmpty() && !password.trim().isEmpty()) {
			String query =
					"MATCH (n:User) " +
					"WHERE n.login={login} AND NOT(n.password IS NULL) " +
					"AND (NOT(HAS(n.blocked)) OR n.blocked = false) ";
			if (checkFederatedLogin) {
				query += "AND (NOT(HAS(n.federated)) OR n.federated = false) ";
			}
			query +=
					"OPTIONAL MATCH (p:Profile) " +
					"WHERE HAS(n.profiles) AND p.name = head(n.profiles) " +
					"RETURN DISTINCT n.id as userId, n.password as password, p.blocked as blockedProfile";
			Map<String, Object> params = new HashMap<>();
			params.put("login", username);
			neo.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonArray result = res.body().getJsonArray("result");
					if ("ok".equals(res.body().getString("status")) &&
							result != null && result.size() == 1) {
						JsonObject r = result.getJsonObject(0);
						String dbPassword;
						if (r != null && (dbPassword = r.getString("password")) != null && !getOrElse(r.getBoolean("blockedProfile"), false)) {
							boolean success = false;
							String hash = null;
							try {
								switch (dbPassword.length()) {
									case 32: // md5
										hash = Md5.hash(password);
										break;
									case 64: // sha-256
										hash = Sha256.hash(password);
										break;
									default: // BCrypt
										success = BCrypt.checkpw(password, dbPassword);
								}
								if (!success && hash != null) {
									success = !dbPassword.trim().isEmpty() && dbPassword.equalsIgnoreCase(hash);
									if (success) {
										upgradeOldPassword(username, password);
									}
								}
							} catch (NoSuchAlgorithmException e) {
								log.error(e.getMessage(), e);
							}
							if (success) {
								handler.handle(r.getString("userId"));
							} else {
								handler.handle(null);
							}
						} else {
							handler.handle(null);
						}
					} else {
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(null);
		}
	}

	private void upgradeOldPassword(final String username, String password) {
		String query =
				"MATCH (u:User {login: {login}}) SET u.password = {password} " +
				"RETURN u.id as id, HEAD(u.profiles) as profile ";
		JsonObject params = new JsonObject()
				.put("login", username)
				.put("password", BCrypt.hashpw(password, BCrypt.gensalt()));
		neo.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error updating old password for user " + username + " : " + event.body().getString("message"));
				} else if (event.body().getJsonArray("result") != null && event.body().getJsonArray("result").size() == 1) {
					// welcome message
					JsonObject message = new JsonObject()
							.put("userId", event.body().getJsonArray("result").getJsonObject(0).getString("id"))
							.put("profile", event.body().getJsonArray("result").getJsonObject(0).getString("profile"))
							.put("request", new JsonObject()
									.put("headers", new JsonObject()
													.put("Accept-Language", getRequest().getHeader("Accept-Language"))
													.put("Host", getRequest().getHeader("Host"))
													.put("X-Forwarded-Host", getRequest().getHeader("X-Forwarded-Host"))
									)
							);
					neo.getEventBus().publish("send.welcome.message", message);
				}
			}
		});
	}

	@Override
	public void createOrUpdateAuthInfo(String clientId, String userId,
			String scope, Handler<AuthInfo> handler) {
		createOrUpdateAuthInfo(clientId, userId, scope, null, handler);
	}

	public void createOrUpdateAuthInfo(final String clientId, final String userId,
			final String scope, final String redirectUri, final Handler<AuthInfo> handler) {
		if (clientId != null && userId != null &&
				!clientId.trim().isEmpty() && !userId.trim().isEmpty()) {
			if (scope != null && !scope.trim().isEmpty()) {
				String query = "MATCH (app:`Application` {name:{clientId}}) RETURN app.scope as scope";
				neo.execute(query, new JsonObject().put("clientId", clientId),
						new io.vertx.core.Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						JsonArray r = res.body().getJsonArray("result");

						if ("ok".equals(res.body().getString("status")) &&
								r != null && r.size() == 1) {
							JsonObject j = r.getJsonObject(0);
							if (j != null &&
								j.getJsonArray("scope", new JsonArray()).getList()
										.containsAll(Arrays.asList(scope.split("\\s")))) {
								createAuthInfo(clientId, userId, scope, redirectUri, handler);
							} else {
								handler.handle(null);
							}
						} else {
							handler.handle(null);
						}
					}
				});
			} else {
				createAuthInfo(clientId, userId, scope, redirectUri, handler);
			}
		} else {
			handler.handle(null);
		}
	}

	private void createAuthInfo(String clientId, String userId, String scope,
			String redirectUri, final Handler<AuthInfo> handler) {
		final JsonObject auth = new JsonObject()
		.put("clientId", clientId)
		.put("userId", userId)
		.put("scope", scope)
		.put("createdAt", MongoDb.now());
		if (redirectUri != null) {
			auth.put("redirectUri", redirectUri)
			.put("code", UUID.randomUUID().toString())
			.put("refreshToken", UUID.randomUUID().toString());
		}
		mongo.save(AUTH_INFO_COLLECTION, auth, new io.vertx.core.Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					auth.put("id", res.body().getString("_id"));
					auth.remove("createdAt");
					ObjectMapper mapper = new ObjectMapper();
					try {
						handler.handle(mapper.readValue(auth.encode(), AuthInfo.class));
					} catch (IOException e) {
						handler.handle(null);
					}
				} else {
					handler.handle(null);
				}
			}
		});
	}

	@Override
	public void createOrUpdateAccessToken(final AuthInfo authInfo, final Handler<AccessToken> handler) {
		if (authInfo != null) {
			final JsonObject query = new JsonObject().put("authId", authInfo.getId());
			mongo.count(ACCESS_TOKEN_COLLECTION, query,
					new io.vertx.core.Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status")) &&
							event.body().getInteger("count", 1) == 0) {
						final JsonObject token = new JsonObject()
								.put("authId", authInfo.getId())
								.put("token", UUID.randomUUID().toString())
								.put("createdOn", MongoDb.now())
								.put("expiresIn", 3600);
						if (openIdConnectService != null && authInfo.getScope() != null && authInfo.getScope().contains("openid")) {
						//"2.0".equals(RequestUtils.getAcceptVersion(getRequest().getHeader("Accept")))) {
							openIdConnectService.generateIdToken(authInfo.getUserId(), authInfo.getClientId(), new io.vertx.core.Handler<AsyncResult<String>>() {
								@Override
								public void handle(AsyncResult<String> ar) {
									if (ar.succeeded()) {
										token.put("id_token", ar.result());
										persistToken(token);
									} else {
										log.error("Error generating id_token.", ar.cause());
										handler.handle(null);
									}
								}
							});
						} else {
							persistToken(token);
						}
					} else { // revoke existing token and code with same authId
						mongo.delete(ACCESS_TOKEN_COLLECTION, query);
						mongo.delete(AUTH_INFO_COLLECTION,
								new JsonObject().put("_id", authInfo.getId()));
						handler.handle(null);
					}
				}

				private void persistToken(final JsonObject token) {
					mongo.save(ACCESS_TOKEN_COLLECTION, token,
							new io.vertx.core.Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
								AccessToken t = new AccessToken();
								t.setAuthId(authInfo.getId());
								t.setToken(token.getString("token"));
								t.setCreatedOn(new Date(token.getJsonObject("createdOn").getLong("$date")));
								t.setExpiresIn(3600);
								if (token.containsKey("id_token")) {
									t.setIdToken(token.getString("id_token"));
								}
								handler.handle(t);
							} else {
								handler.handle(null);
							}
						}
					});
				}
			});
		} else {
			handler.handle(null);
		}
	}

	@Override
	public void getAuthInfoByCode(String code, final Handler<AuthInfo> handler) {
		if (code != null && !code.trim().isEmpty()) {
			JsonObject query = new JsonObject()
			.put("code", code)
			.put("createdAt", new JsonObject()
					.put("$gte",
							new JsonObject().put("$date", System.currentTimeMillis() - CODE_EXPIRES)));
			mongo.findOne(AUTH_INFO_COLLECTION, query, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonObject r = res.body().getJsonObject("result");
					if ("ok".equals(res.body().getString("status")) && r != null && r.size() > 0) {
						r.put("id", r.getString("_id"));
						r.remove("_id");
						r.remove("createdAt");
						ObjectMapper mapper = new ObjectMapper();
						try {
							handler.handle(mapper.readValue(r.encode(), AuthInfo.class));
						} catch (IOException e) {
							handler.handle(null);
						}
					} else {
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(null);
		}
	}

	@Override
	public void getAuthInfoByRefreshToken(String refreshToken,
			Handler<AuthInfo> handler) {
		throw new IllegalArgumentException("Not implemented yet.");
	}

	@Override
	public void getClientUserId(String clientId, String clientSecret,
			Handler<String> handler) {
		handler.handle("OAuthSystemUser");
	}

	@Override
	public void validateClientById(String clientId, final Handler<Boolean> handler) {
		if (clientId != null && !clientId.trim().isEmpty()) {
			String query =
					"MATCH (n:Application) " +
					"WHERE n.name = {clientId} " +
					"RETURN count(n) as nb";
			Map<String, Object> params = new HashMap<>();
			params.put("clientId", clientId);
			neo.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonArray a = res.body().getJsonArray("result");
					if ("ok".equals(res.body().getString("status")) && a != null && a.size() == 1) {
						JsonObject r = a.getJsonObject(0);
						handler.handle(r != null && r.getInteger("nb") == 1);
					} else {
						handler.handle(false);
					}
				}
			});
		} else {
			handler.handle(false);
		}
	}

	@Override
	public void validateUserById(String userId, final Handler<Boolean> handler) {
		if (userId != null && !userId.trim().isEmpty()) {
			String query =
					"MATCH (n:User) " +
					"WHERE n.id = {userId} " +
					"RETURN count(n) as nb";
			Map<String, Object> params = new HashMap<>();
			params.put("userId", userId);
			neo.execute(query, params, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonArray a = res.body().getJsonArray("result");
					if ("ok".equals(res.body().getString("status")) && a != null && a.size() == 1) {
						JsonObject r = a.getJsonObject(0);
						handler.handle(r != null && r.getInteger("nb") == 1);
					} else {
						handler.handle(false);
					}
				}
			});
		} else {
			handler.handle(false);
		}
	}

	@Override
	public void getAccessToken(String token, final Handler<AccessToken> handler) {
		if (token != null && !token.trim().isEmpty()) {
			JsonObject query = new JsonObject()
			.put("token", token);
			mongo.findOne(ACCESS_TOKEN_COLLECTION, query, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonObject r = res.body().getJsonObject("result");
					if ("ok".equals(res.body().getString("status")) &&
							r != null && r.size() > 0) {
						AccessToken t = new AccessToken();
						t.setAuthId(r.getString("authId"));
						t.setToken(r.getString("token"));
						t.setCreatedOn(MongoDb.parseIsoDate(r.getJsonObject("createdOn")));
						t.setExpiresIn(r.getInteger("expiresIn"));
						handler.handle(t);
					} else {
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(null);
		}
	}

	@Override
	public void getAuthInfoById(String id, final Handler<AuthInfo> handler) {
		if (id != null && !id.trim().isEmpty()) {
			JsonObject query = new JsonObject()
			.put("_id", id);
			mongo.findOne(AUTH_INFO_COLLECTION, query, new io.vertx.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getJsonObject("result");
						r.put("id", r.getString("_id"));
						r.remove("_id");
						r.remove("createdAt");
						ObjectMapper mapper = new ObjectMapper();
						try {
							handler.handle(mapper.readValue(r.encode(), AuthInfo.class));
						} catch (IOException e) {
							handler.handle(null);
						}
					} else {
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(null);
		}
	}

}
