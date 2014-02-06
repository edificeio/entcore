package org.entcore.auth.oauth;

import java.io.IOException;
import java.util.*;

import jp.eisbahn.oauth2.server.async.Handler;
import jp.eisbahn.oauth2.server.data.DataHandler;
import jp.eisbahn.oauth2.server.models.AccessToken;
import jp.eisbahn.oauth2.server.models.AuthInfo;
import jp.eisbahn.oauth2.server.models.Request;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.one.core.infra.MongoDb;
import org.entcore.common.neo4j.Neo;
import edu.one.core.infra.security.BCrypt;

public class OAuthDataHandler extends DataHandler {

	private final Neo neo;
	private final MongoDb mongo;
	private static final String AUTH_INFO_COLLECTION = "authorizations";
	private static final String ACCESS_TOKEN_COLLECTION = "tokens";
	private static final int CODE_EXPIRES = 600000; // 10 min

	public OAuthDataHandler(Request request, Neo neo, MongoDb mongo) {
		super(request);
		this.neo = neo;
		this.mongo = mongo;
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
		neo.send(query, params, new org.vertx.java.core.Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject r = res.body().getObject("result").getObject("0");
					if (r != null && "1".equals(r.getString("nb"))) {
						handler.handle(true);
					} else {
						handler.handle(false);
					}
				} else {
					handler.handle(false);
				}
			}
		});
	}

	@Override
	public void getUserId(String username, final String password, final Handler<String> handler) {
		if (username != null && password != null &&
				!username.trim().isEmpty() && !password.trim().isEmpty()) {
			String query =
					"MATCH (n:User) " +
					"WHERE n.login={login} AND NOT(n.password IS NULL) " +
					"AND (NOT(HAS(n.blocked)) OR n.blocked = false) " +
					"RETURN n.id as userId, n.password as password";
			Map<String, Object> params = new HashMap<>();
			params.put("login", username);
			neo.send(query, params, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonObject result = res.body().getObject("result");
					if ("ok".equals(res.body().getString("status")) &&
							result != null && result.size() == 1) {
						JsonObject r = result.getObject("0");
						if (r != null && BCrypt.checkpw(password, r.getString("password"))) {
							handler.handle(r.getString("userId"));
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
				neo.execute(query, new JsonObject().putString("clientId", clientId),
						new org.vertx.java.core.Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						JsonArray r = res.body().getArray("result");

						if ("ok".equals(res.body().getString("status")) &&
								r != null && r.size() == 1) {
							JsonObject j = r.get(0);
							if (j != null &&
								Arrays.asList(j.getArray("scope", new JsonArray()).toArray())
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
		.putString("clientId", clientId)
		.putString("userId", userId)
		.putString("scope", scope)
		.putObject("createdAt", MongoDb.now());
		if (redirectUri != null) {
			auth.putString("redirectUri", redirectUri)
			.putString("code", UUID.randomUUID().toString())
			.putString("refreshToken", UUID.randomUUID().toString());
		}
		mongo.save(AUTH_INFO_COLLECTION, auth, new org.vertx.java.core.Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					auth.putString("id", res.body().getString("_id"));
					auth.removeField("createdAt");
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
			final JsonObject query = new JsonObject().putString("authId", authInfo.getId());
			mongo.count(ACCESS_TOKEN_COLLECTION, query,
					new org.vertx.java.core.Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status")) &&
							event.body().getNumber("count", 1) == 0) {
						final JsonObject token = new JsonObject()
								.putString("authId", authInfo.getId())
								.putString("token", UUID.randomUUID().toString())
								.putObject("createdOn", MongoDb.now())
								.putNumber("expiresIn", 3600);
						mongo.save(ACCESS_TOKEN_COLLECTION, token,
								new org.vertx.java.core.Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> res) {
								if ("ok".equals(res.body().getString("status"))) {
									AccessToken t = new AccessToken();
									t.setAuthId(authInfo.getId());
									t.setToken(token.getString("token"));
									t.setCreatedOn(new Date(token.getObject("createdOn").getLong("$date")));
									t.setExpiresIn(3600);
									handler.handle(t);
								} else {
									handler.handle(null);
								}
							}
						});
					} else { // revoke existing token and code with same authId
						mongo.delete(ACCESS_TOKEN_COLLECTION, query);
						mongo.delete(AUTH_INFO_COLLECTION,
								new JsonObject().putString("_id", authInfo.getId()));
						handler.handle(null);
					}
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
			.putString("code", code)
			.putObject("createdAt", new JsonObject()
					.putObject("$gte",
							new JsonObject().putNumber("$date", System.currentTimeMillis() - CODE_EXPIRES)));
			mongo.findOne(AUTH_INFO_COLLECTION, query, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonObject r = res.body().getObject("result");
					if ("ok".equals(res.body().getString("status")) && r != null && r.size() > 0) {
						r.putString("id", r.getString("_id"));
						r.removeField("_id");
						r.removeField("createdAt");
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
			neo.send(query, params, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getObject("result").getObject("0");
						if (r != null && "1".equals(r.getString("nb"))) {
							handler.handle(true);
						} else {
							handler.handle(false);
						}
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
			neo.send(query, params, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getObject("result").getObject("0");
						if (r != null && "1".equals(r.getString("nb"))) {
							handler.handle(true);
						} else {
							handler.handle(false);
						}
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
			.putString("token", token);
			mongo.findOne(ACCESS_TOKEN_COLLECTION, query, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					JsonObject r = res.body().getObject("result");
					if ("ok".equals(res.body().getString("status")) &&
							r != null && r.size() > 0) {
						AccessToken t = new AccessToken();
						t.setAuthId(r.getString("authId"));
						t.setToken(r.getString("token"));
						t.setCreatedOn(MongoDb.parseIsoDate(r.getObject("createdOn")));
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
			.putString("_id", id);
			mongo.findOne(AUTH_INFO_COLLECTION, query, new org.vertx.java.core.Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getObject("result");
						r.putString("id", r.getString("_id"));
						r.removeField("_id");
						r.removeField("createdAt");
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
