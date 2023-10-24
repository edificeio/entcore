/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.session;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.lang3.tuple.Pair;
import org.entcore.common.cache.CacheService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.redis.Redis;
import org.entcore.common.session.SessionRecreationRequest;
import org.entcore.common.utils.StringUtils;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthManager extends BusModBase implements Handler<Message<JsonObject>> {

	public static final String SESSIONS_COLLECTION = "sessions";
	public static final String OAUTH_AUTH_INFO_COLLECTION = "authorizations";
	public static final String OAUTH_ACCESS_TOKEN_COLLECTION = "tokens";
	public static final String CAS_COLLECTION = "authcas";

	protected MongoDb mongo;
	protected Neo4j neo4j;
	protected SessionStore sessionStore;
	protected CacheService OAuthCacheService;
	protected Boolean cluster;
	protected boolean xsrfOnAuth;

	public void start() {
		super.start();
		LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");

		String neo4jConfig = (String) server.get("neo4jConfig");
		neo4j = Neo4j.getInstance();
		neo4j.init(vertx, new JsonObject(neo4jConfig));

		cluster = (Boolean) server.get("cluster");
		String node = (String) server.get("node");
		mongo = MongoDb.getInstance();
		mongo.init(vertx.eventBus(), node + config.getString("mongo-address", "wse.mongodb.persistor"));

		sessionStore = new MapSessionStore(vertx, cluster, config);

		this.xsrfOnAuth = config.getBoolean("xsrfOnAuth", true);

		try
		{
			Object oauthCacheConf = server.get("oauthCache");
			if(oauthCacheConf != null)
			{
				JsonObject redisConfig = new JsonObject((String) server.get("redisConfig"));
				if(new JsonObject((String)oauthCacheConf).getBoolean("enabled", false) == true)
				{
					Redis.getInstance().init(vertx, redisConfig);
					this.OAuthCacheService = CacheService.create(vertx);
				}
			}
		}
		catch(Exception e)
		{
			logger.error("Failed to create OAuthCacheService: " + e.getMessage());
		}

		final String address = getOptionalStringConfig("address", "wse.session");
		eb.localConsumer(address, this);
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action");

		if (action == null) {
			sendError(message, "action must be specified");
			return;
		}

		switch (action) {
		case "find":
			doFind(message);
			break;
		case "findByUserId":
			doFindByUserId(message, null);
			break;
		case "create":
			doCreate(message);
			break;
		case "recreate":
			doReCreate(message);
			break;
		case "dropAllByUserId":
			dropAllByUserId(message);
			break;
		case "drop":
			doDrop(message);
			break;
		case "dropByUserId":
			doDropByUserId(message);
			break;
		case "dropCacheSession":
			doDropCacheSession(message);
			break;
		case "dropPermanentSessions" :
			doDropPermanentSessions(message);
			break;
		case "addAttribute":
			doAddAttribute(message);
			break;
		case "addAttributeOnSessionId":
			doAddAttributeOnSessionId(message);
			break;
		case "removeAttribute":
			doRemoveAttribute(message);
			break;
		case "sessionNumber":
			doSessionNumber(message);
			break;
		default:
			sendError(message, "Invalid action: " + action);
		}
	}

	private void doSessionNumber(Message<JsonObject> message) {
		sessionStore.getSessionsNumber(ar -> {
			if (ar.succeeded()) {
				sendOK(message, new JsonObject().put("count", ar.result()));
			} else {
				logger.error("Error when get session number", ar.cause());
				sendError(message, "Error when get session number");
			}
		});
	}

	public void dropAllByUserId(Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doDropCacheSession] Invalid userId : " + message.body().encode());
			return;
		}
		String uid = message.body().getString("userId");
		sessionStore.listSessionsIds(uid, ar -> {
			if (ar.succeeded()) {
				JsonArray removedSessions = new JsonArray();
				for (Object sessionId : ar.result()) {
					if (sessionId instanceof String) {
						removedSessions.add((String) sessionId);
						JsonObject json = new JsonObject().put("_id", sessionId).put("userId", userId);
						mongo.delete(SESSIONS_COLLECTION, json);
						dropSession(null, (String) sessionId, null);
					}
				}
				sendOK(message, new JsonObject().put("dropped", removedSessions));
			} else {
				logger.error("[doDropCacheSession] error when list sessions ids with userId : " + userId, ar.cause());
				sendError(message, "[doDropCacheSession] Invalid userId : " + userId);
			}
		});
	}

	private void doDropCacheSession(Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doDropCacheSession] Invalid userId : " + message.body().encode());
			return;
		}
		String currentSessionId = message.body().getString("currentSessionId");
		sessionStore.listSessionsIds(userId, ar -> {
			if (ar.succeeded()) {
				JsonArray removedSessions = new JsonArray();
				for (Object sessionId : ar.result()) {
					if (sessionId instanceof String) {
						if(currentSessionId == null || ((String)sessionId).equals(currentSessionId) == false)
						{
							removedSessions.add((String) sessionId);
							dropSession(null, (String) sessionId, null);
						}
					}
				}
				sendOK(message, new JsonObject().put("dropped", removedSessions));
			} else {
				logger.error("[doDropCacheSession] error when list sessions ids with userId : " + userId, ar.cause());
				sendError(message, "[doDropCacheSession] Invalid userId : " + userId);
			}
		});
	}

	private void doDropPermanentSessions(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		String currentSessionId = message.body().getString("currentSessionId");
		String currentTokenId = message.body().getString("currentTokenId");
		boolean immediate = getOrElse(message.body().getBoolean("immediate"), true);

		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doDropPermanentSessions] Invalid userId : " + message.body().encode());
			return;
		}

		Handler<Either<String, Void>> finalHandler = event ->
		{
			if (event.isRight()) {
				sendOK(message);
			} else {
				sendError(message, event.left().getValue());
			}
		};

		Handler<Message<JsonObject>> dropSessionsHandler = new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> msg)
			{
				if("ok".equals(msg.body().getString("status")) == false)
					finalHandler.handle(new Either.Left<String, Void>(msg.body().getString("message")));
				else
					dropOAuth2Tokens(userId, currentTokenId, immediate, finalHandler);
			}
		};

		JsonObject query = new JsonObject().put("userId", userId);
		if (currentSessionId != null) {
			query.put("_id", new JsonObject().put("$ne", currentSessionId));
		}

		if (immediate) {
			mongo.delete(SESSIONS_COLLECTION, query, dropSessionsHandler);
		} else {
			// set a TTL flag to tell background task to delete session (TTL index expireAfterSeconds: 60 seconds)
			mongo.update(SESSIONS_COLLECTION,
					query,
					new MongoUpdateBuilder().set("flagTTL", MongoDb.now()).build(),
					false,
					true,
					dropSessionsHandler);
		}
	}

	private void dropOAuth2Tokens(String userId, String currentTokenId, boolean immediate, Handler<Either<String, Void>> handler)
	{
		JsonObject userFilter = new JsonObject().put("userId", userId);
		JsonObject setTTL = new MongoUpdateBuilder().set("flagTTL", MongoDb.now()).build();
		mongo.find(OAUTH_AUTH_INFO_COLLECTION, userFilter, new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> result)
			{
				if("ok".equals(result.body().getString("status")) == false)
					handler.handle(new Either.Left<String, Void>(result.body().getString("message")));
				else
				{
					JsonArray resArray = result.body().getJsonArray("results");
					JsonArray authIds = new JsonArray();
					JsonObject authIdFilter = new JsonObject().put("authId", new JsonObject().put("$in", authIds));
					for(int i = resArray.size(); i-- > 0;)
						authIds.add(resArray.getJsonObject(i).getString("_id"));

					Handler<Message<JsonObject>> authHandler = new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> authRes)
						{
							if("ok".equals(authRes.body().getString("status")) == false)
								handler.handle(new Either.Left<String, Void>(authRes.body().getString("message")));
							else
								dropCASTokens(userId, immediate, handler);
						}
					};
					Handler<Message<JsonObject>> tokenHandler = new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> tokenRes)
						{
							if("ok".equals(tokenRes.body().getString("status")) == false)
								handler.handle(new Either.Left<String, Void>(tokenRes.body().getString("message")));
							else
							{
								if(immediate)
									mongo.delete(OAUTH_AUTH_INFO_COLLECTION, userFilter, authHandler);
								else
									// set a TTL flag to tell background task to delete session (TTL index expireAfterSeconds: 60 seconds)
									mongo.update(OAUTH_AUTH_INFO_COLLECTION, userFilter, setTTL, false, true, authHandler);
							}
						}
					};

					mongo.find(OAUTH_ACCESS_TOKEN_COLLECTION, authIdFilter, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> result)
						{
							if("ok".equals(result.body().getString("status")) == false)
								handler.handle(new Either.Left<String, Void>(result.body().getString("message")));
							else
							{
								JsonArray resArray = result.body().getJsonArray("results");

								for(int i = resArray.size(); i-- > 0;)
								{
									String tokenId = resArray.getJsonObject(i).getString("token");
									if(tokenId.equals(currentTokenId))
									{
										// Keep this token and its auth alive
										authIdFilter.put("token", new JsonObject().put("$ne", currentTokenId));
										userFilter.put("_id", new JsonObject().put("$ne", resArray.getJsonObject(i).getString("authId")));
										continue;
									}

									if(OAuthCacheService != null)
									{
										String tokenKey = "AppOAuthResourceProvider:token:" + tokenId;
										OAuthCacheService.remove(tokenKey, new Handler<AsyncResult<Void>>()
										{
											@Override
											public void handle(AsyncResult<Void> res)
											{
												if(res.succeeded() == false)
													logger.error("[dropOAuth2Tokens] Failed to remove cached token " + tokenKey);
											}
										});
									}
								}

								if(immediate)
									mongo.delete(OAUTH_ACCESS_TOKEN_COLLECTION, authIdFilter, tokenHandler);
								else
									// set a TTL flag to tell background task to delete session (TTL index expireAfterSeconds: 60 seconds)
									mongo.update(OAUTH_ACCESS_TOKEN_COLLECTION, authIdFilter, setTTL, false, true, tokenHandler);
							}
						}
					});
				}
			}
		});
	}

	private void dropCASTokens(String userId, boolean immediate, Handler<Either<String, Void>> handler)
	{
		JsonObject userFilter = new JsonObject().put("user", userId);

		Handler<Message<JsonObject>> casHandler = new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> authRes)
			{
				if("ok".equals(authRes.body().getString("status")) == false)
					handler.handle(new Either.Left<String, Void>(authRes.body().getString("message")));
				else
					handler.handle(new Either.Right<String, Void>(null));
			}
		};

		if (immediate)
			mongo.delete(CAS_COLLECTION, userFilter, casHandler);
		else
			// set a TTL flag to tell background task to delete session (TTL index expireAfterSeconds: 60 seconds)
			mongo.update(CAS_COLLECTION, userFilter, new MongoUpdateBuilder().set("flagTTL", MongoDb.now()).build(), false, true, casHandler);
	}

	private void doFindByUserId(final Message<JsonObject> message, final Handler<JsonObject> callHandlerInsteadOfMessageReply) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			if(callHandlerInsteadOfMessageReply != null)
				callHandlerInsteadOfMessageReply.handle(null);
			else
				sendError(message, "[doFindByUserId] Invalid userId : " + message.body().encode());
			return;
		}

		sessionStore.getSessionByUserId(userId, ar -> {
			if (ar.succeeded()) {
				if(callHandlerInsteadOfMessageReply != null)
					callHandlerInsteadOfMessageReply.handle(ar.result());
				else
					sendOK(message, new JsonObject().put("status", "ok").put("session", ar.result()));
			} else if (getOrElse(message.body().getBoolean("allowDisconnectedUser"), false)) {
				generateSessionInfos(userId, new Handler<JsonObject>() {

					@Override
					public void handle(JsonObject infos) {
						if (infos != null) {
							if(callHandlerInsteadOfMessageReply != null)
								callHandlerInsteadOfMessageReply.handle(infos);
							else
								sendOK(message, new JsonObject().put("status", "ok")
									.put("session", infos));
						} else {
							if(callHandlerInsteadOfMessageReply != null)
								callHandlerInsteadOfMessageReply.handle(null);
							else
								sendError(message, "Invalid userId : " + userId);
						}
					}
				});
			} else {
				if(callHandlerInsteadOfMessageReply != null)
					callHandlerInsteadOfMessageReply.handle(null);
				else
					sendError(message, "[doFindByUserId] info is null - Invalid userId : " + message.body().encode());
			}
		});
	}

	private void doFind(final Message<JsonObject> message) {
		final String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		sessionStore.getSession(sessionId, ar -> {
			if (ar.succeeded()) {
				sendOK(message, new JsonObject().put("status", "ok").put("session", ar.result()));
			} else if (!sessionStore.inactivityEnabled()) {
				final JsonObject query = new JsonObject().put("_id", sessionId);
				mongo.findOne(SESSIONS_COLLECTION, query, event -> {
					JsonObject res = event.body().getJsonObject("result");
					String userId;
					if ("ok".equals(event.body().getString("status")) && res != null &&
							(userId = res.getString("userId")) != null && !userId.trim().isEmpty()) {
						final String uId = userId;
						final boolean secureLocation = getOrElse(res.getBoolean("secureLocation"), false);
						createSession(userId, sessionId, res.getString("SessionIndex"), res.getString("NameID"), secureLocation, null,
								sId -> {
									if (sId != null) {
										sessionStore.getSession(sId, ar2 -> {
											if (ar2.succeeded()) {
												JsonObject s = ar2.result();
												if (s != null) {
													JsonObject sessionResponse = new JsonObject().put("status", "ok")
															.put("session", s);
													sendOK(message, sessionResponse);
												} else {
													sendError(message, "Session not found. 1");
												}
											} else {
												generateSessionInfos(uId, event1 -> {
													if (event1 != null) {
														logger.info("Session with store problem : " + event1.encode());
														sendOK(message, new JsonObject().put("status", "ok")
																.put("session", event1));
													} else {
														sendError(message, "Session not found. 2");
													}
												});
											}
										});
									} else {
										sendError(message, "Session not found. 3");
									}
								});
					} else {
						message.reply(new JsonObject().put("status", "error").put("message", "Session not found. 4"));
					}
				});
			} else {
				message.reply(new JsonObject().put("status", "error").put("message", "Session not found. 5 (with inactivity enabled)"));
			}
		});
	}

	/**
	 * Recreate a session for the current user and then remove the current session from the store.
	 * @param message Recreate session request received from the bus
	 */
	private void doReCreate(final Message<JsonObject> message) {
		final SessionRecreationRequest request = message.body().mapTo(SessionRecreationRequest.class);
		final String userId = request.getUserId();
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doReCreate] Invalid userId : " + message.body());
			return;
		}
		sessionStore.getSession(request.getSessionId(), result -> {
			final String nameId;
			final String sessionIndex;
			final boolean secureLocation;
			final JsonObject cache;
			if(result.succeeded()) {
				final JsonObject oldSession = result.result();
				final JsonObject sessionMetadata = oldSession.getJsonObject("sessionMetadata");
				sessionIndex = sessionMetadata.getString("SessionIndex");
				nameId = sessionMetadata.getString("NameID");
				secureLocation = Boolean.TRUE.equals(sessionMetadata.getBoolean("secureLocation"));
				cache = oldSession.getJsonObject("cache");
			} else {
				sessionIndex = null;
				nameId = null;
				secureLocation = false;
				cache = null;
			}
			createSession(userId, request.isRefreshOnly() ? request.getSessionId() : null, sessionIndex, nameId, secureLocation, cache)
			.onSuccess(session -> {
				message.reply(session);
				final String sessionId = request.getSessionId();
				// TODO update metrics
				if(sessionId != null && !request.isRefreshOnly()) {
					sessionStore.dropSession(sessionId, dropSessionResult -> {
						if(dropSessionResult.succeeded()) {
							logger.debug("Successfully deleted " + userId+ "'s old session");
						} else {
							logger.info("Error while deleting " + userId+ "'s old session", dropSessionResult.cause());
						}
					});
				}
			})
			.onFailure(th -> sendError(message, "[doReCreate] Error while recreating the session", th));
		});
	}
	private void doCreate(final Message<JsonObject> message) {
		final JsonObject body = message.body();
		final String userId = body.getString("userId");
		final String sessionIndex = body.getString("SessionIndex");
		final String nameID = body.getString("NameID");
		final String desiredSessionId = body.getString("sessionId");
		final boolean secureLocation = body.getBoolean("secureLocation", false);
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doCreate] Invalid userId : " + body.encode());
			return;
		}

		createSession(userId, desiredSessionId, sessionIndex, nameID, secureLocation, null, sessionId -> {
			if (sessionId != null) {
				sendOK(message, new JsonObject()
						.put("status", "ok")
						.put("sessionId", sessionId)
						.put("xsrf", xsrfOnAuth ? UUID.randomUUID().toString() : null)
				);
			} else {
				sendError(message, "Invalid userId : " + userId);
			}
		});
	}

	/**
	 * Creates a session for a user.
	 * @param userId Id of the user for whom we want to create a session
	 * @param sId Desired session id
	 * @param sessionIndex used in identity federation
	 * @param nameId used in identity federation
	 * @param secureLocation {@code true} if the session is from a secure location
	 * @param previousCache Cache of the previous session
	 * @return The created session objct
	 */
	private Future<JsonObject> createSession(final String userId, final String sId, final String sessionIndex, final String nameId,
											 final boolean secureLocation, final JsonObject previousCache) {
		return createSessionAndReturnIdAndData(userId, sId, sessionIndex, nameId, secureLocation, previousCache)
			.map(result -> result.getRight());
	}


	/**
	 * Creates a session for a user.
	 * @param userId Id of the user for whom we want to create a session
	 * @param sId Desired session id
	 * @param sessionIndex used in identity federation
	 * @param nameId used in identity federation
	 * @param secureLocation {@code true} if the session is from a secure location
	 * @param previousCache Cache of the previous session
	 * @param handler Action to be called with the session id of the newly created session ({@code null} will be passed if we could not create the session)
	 * @return The created session object
	 */
	private void createSession(final String userId, final String sId, final String sessionIndex, final String nameId,
							   final boolean secureLocation, final JsonObject previousCache, final Handler<String> handler) {
		createSessionAndReturnIdAndData(userId, sId, sessionIndex, nameId, secureLocation, previousCache)
			.onComplete(result -> {
				if(result.succeeded()) {
					handler.handle(result.result().getLeft());
				} else {
					handler.handle(null);
				}
			});
	}


	/**
	 * Creates a session for a user.
	 * @param userId Id of the user for whom we want to create a session
	 * @param sId Desired session id
	 * @param sessionIndex used in identity federation
	 * @param nameId used in identity federation
	 * @param secureLocation {@code true} if the session is from a secure location
	 * @param previousCache Cache of the previous session
	 * @return The id of the created session object along with its data.<br /><strong><u>NB : </u>NB</strong> Note that
	 * data may be null if the user could not be found in Neo4J. It is the case for OAuth client_creddentials identification
	 * for instance.
	 */
	private Future<Pair<String, JsonObject>> createSessionAndReturnIdAndData(final String userId, final String sId, final String sessionIndex, final String nameId,
												  final boolean secureLocation, final JsonObject previousCache) {
		final Promise<Pair<String, JsonObject>> sessionPromise = Promise.promise();
		final String sessionId = (sId != null) ? sId : UUID.randomUUID().toString();
		generateSessionInfos(userId, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject infos) {
				if (infos != null) {
					final JsonObject json = new JsonObject().put("_id", sessionId).put("userId", userId);
					if (sessionIndex != null && nameId != null) {
						json.put("SessionIndex", sessionIndex).put("NameID", nameId);
					}
					if (secureLocation) {
						json.put("secureLocation", secureLocation);
					}
					infos.put("sessionMetadata", json);
					// Because some implementations of sessionStore.putSession can modify the session, we need to
					// duplicate it to return it to the caller
					final JsonObject sessionToReturn = infos.copy();
					sessionStore.putSession(userId, sessionId, infos, secureLocation, ar -> {
						if (ar.failed()) {
							logger.error("Error putting session in store", ar.cause());
						}
						addOldCacheValuesToNewSession(previousCache, userId, sessionId).onComplete(e -> {
							if (!sessionStore.inactivityEnabled()) {
								final JsonObject now = MongoDb.now();
								if (sId == null) {
									json.put("created", now).put("lastUsed", now);
									mongo.save(SESSIONS_COLLECTION, json);
								} else {
									mongo.update(SESSIONS_COLLECTION, new JsonObject().put("_id", sessionId),
											new JsonObject().put("$set", new JsonObject().put("lastUsed", now)));
								}
							}
							sessionPromise.complete(Pair.of(sessionId, sessionToReturn));
						});
					});
				} else {
					sessionPromise.complete(Pair.of(sessionId, null));
				}
			}
		});
		return sessionPromise.future();
	}

	/**
	 * <p>
	 * Puts the cache attributes of a previous session into the cache attribute of a new session.
	 * This action is required because now (06/03/2023) different implementations of SessionStore
	 * have different behaviours while handling session's cache (RedisSession removes it when storing it
	 * a session via putSession).
	 *</p>
	 * <p>
	 *     <u>NB :</u> This works well when Redis is not clustered ior when there is only one value in previousCache
	 * </p>
	 * @param previousCache Previous values that were stored in "cache" field of the previous session
	 * @param userId Id of the user whose session is being recreated
	 * @param sessionId Id of the new session
	 * @return A future that completes when the new session's cache has been populated with the old values
	 * 			(but some attributes may have failed to be stored in the new session).
	 */
	private Future<Void> addOldCacheValuesToNewSession(final JsonObject previousCache, final String userId, final String sessionId) {
		final Future<Void> populateCache;
		if(previousCache == null || previousCache.isEmpty()) {
			populateCache = Future.succeededFuture();
		} else {
			logger.debug("Copying previous cached values for user " + userId);
			final Promise<Void> allUpdates = Promise.promise();
			CompositeFuture.join(previousCache.stream().map(previousCacheEntry -> {
				final Promise<Void> addCacheAttribute = Promise.promise();
				final String cacheKey = previousCacheEntry.getKey();
				sessionStore.addCacheAttribute(sessionId, cacheKey, previousCacheEntry.getValue(), res -> {
					if(res.succeeded()) {
						logger.debug(cacheKey + " stored in new session cache for user " + userId);
					} else {
						logger.warn(cacheKey + " could not be stored in new session cache for user " + userId, res.cause());
					}
					addCacheAttribute.complete();
				});
				return addCacheAttribute.future();
			})
			.collect(Collectors.toList()))
			.onComplete(e -> allUpdates.complete());
			populateCache = allUpdates.future();
		}
		return populateCache;
	}

	private void doDrop(final Message<JsonObject> message) {
		final String sessionId = message.body().getString("sessionId");
		boolean sessionMeta =  getOrElse(message.body().getBoolean("sessionMetadata"), false);
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		if (sessionMeta) {
			final JsonObject query = new JsonObject().put("_id", sessionId);
			if (!sessionStore.inactivityEnabled()) {
				mongo.findOne(SESSIONS_COLLECTION, query, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonObject res = event.body().getJsonObject("result");
						dropSession(message, sessionId, res);
					}
				});
			} else {
				dropSession(message, sessionId, new JsonObject());
			}
		} else {
			dropSession(message, sessionId, null);
		}
	}

	private void doDropByUserId(final Message<JsonObject> message)
	{
		doFindByUserId(message, new Handler<JsonObject>()
		{
			@Override
			public void handle(JsonObject session)
			{
				if(session != null)
				{
					message.body().put("sessionId", session.getJsonObject("sessionMetadata").getString("_id"));
					doDrop(message);
				}
				else
					sendOK(message);
			}
		});
	}

	private void dropSession(Message<JsonObject> message, String sessionId, JsonObject meta) {
		sessionStore.dropSession(sessionId, ar -> {
			if (ar.succeeded()) {
				if (getOrElse(config.getBoolean("slo"), false)) {
					final String userId = ar.result().getString("userId");
					eb.request("cas", new JsonObject().put("action", "logout").put("userId", userId));
				}
				if (getOrElse(config.getBoolean("slo-saml"), true)) {
					final String userId = ar.result().getString("userId");
					eb.request("saml", new JsonObject().put("action", "soap-slo").put("sessionId", sessionId).put("userId", userId));
				}
				if (getOrElse(config.getBoolean("slo-oidc-backchannel-logout"), true)) {
					final String userId = ar.result().getString("userId");
					eb.send("openid",
							new JsonObject().put("action", "oidc-slo").put("userId", userId).put("sessionId",
									sessionId));
				}
			} else {
				logger.error("In doDrop - Error getting object after removing hazelcast session " + sessionId, ar.cause());
			}
			JsonObject res = new JsonObject().put("status", "ok");
			if (meta != null) {
				if (meta.isEmpty() && ar.result().getJsonObject("sessionMetadata") != null) {
					res.put("sessionMetadata", ar.result().getJsonObject("sessionMetadata"));
				} else {
					res.put("sessionMetadata", meta);
				}
			}
			if (message != null) {
				sendOK(message, res);
			}
		});
	}

	private void doAddAttributeOnSessionId(Message<JsonObject> message) {
		final String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "[CookieOneSessionId] Invalid sessionId : " + message.body().encode());
			return;
		}
		String key = message.body().getString("key");
		if (key == null || key.trim().isEmpty()) {
			sendError(message, "Invalid key.");
			return;
		}

		Object value = message.body().getValue("value");
		if (value == null || (!(value instanceof String) && !(value instanceof Long) && !(value instanceof JsonObject))) {
			sendError(message, "Invalid value.");
			return;
		}

		sessionStore.addCacheAttribute(sessionId, key, value, ar -> {
			if (ar.succeeded()) {
				sendOK(message);
			} else {
				logger.error("Error adding cache attribute in session", ar.cause());
				sendError(message, "Error adding cache attribute in session");
			}
		});
	}

	private void doAddAttribute(Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[getSessionByUserId] Invalid userId : " + message.body().encode());
			return;
		}
		String key = message.body().getString("key");
		if (key == null || key.trim().isEmpty()) {
			sendError(message, "Invalid key.");
			return;
		}

		Object value = message.body().getValue("value");
		if (value == null || (!(value instanceof String) && !(value instanceof Long) && !(value instanceof JsonObject))) {
			sendError(message, "Invalid value.");
			return;
		}

		sessionStore.addCacheAttributeByUserId(userId, key, value, ar -> {
			if (ar.succeeded()) {
				sendOK(message);
			} else {
				logger.error("Error adding cache attribute in session", ar.cause());
				sendError(message, "Error adding cache attribute in session");
			}
		});
	}

	private void doRemoveAttribute(Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doRemoveAttribute] Invalid userId : " + message.body().encode());
			return;
		}

		String key = message.body().getString("key");
		if (key == null || key.trim().isEmpty()) {
			sendError(message, "Invalid key.");
			return;
		}

		sessionStore.dropCacheAttributeByUserId(userId, key, ar -> {
			if (ar.succeeded()) {
				sendOK(message);
			} else {
				logger.error("Error dropping cache attribute in session", ar.cause());
				sendError(message, "Error dropping cache attribute in session");
			}
		});
	}

	public void generateSessionInfos(final String userId, final Handler<JsonObject> handler) {
		final String query =
				"MATCH (n:User {id : {id}}) " +
				"WHERE HAS(n.login) " +
				"OPTIONAL MATCH n-[:IN]->(gp:Group) " +
				"OPTIONAL MATCH (gp:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH gp-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[rf:HAS_FUNCTION]->(f:Function) " +
				"OPTIONAL MATCH n<-[:RELATED]-(child:User) " +
				"RETURN distinct " +
				"n.classes as classNames, n.level as level, n.email as email, n.mobile as mobile, n.login as login, COLLECT(distinct [c.id, c.name]) as classes, " +
				"n.lastName as lastName, n.firstName as firstName, n.externalId as externalId, n.federated as federated, " +
				"n.birthDate as birthDate, n.changePw as forceChangePassword, COALESCE(n.needRevalidateTerms, FALSE) as needRevalidateTerms,HAS(n.deleteDate) as deletePending, " +
				"n.displayName as username, HEAD(n.profiles) as type, " +
				"COLLECT(distinct [child.id, child.lastName, child.firstName]) as childrenInfo, has(n.password) as hasPw, " +
				"COLLECT(distinct [s.id, s.name, s.UAI, s.hasApp, s.ignoreMFA]) as structures, COLLECT(distinct [f.externalId, rf.scope]) as functions, " +
				"COLLECT(distinct gp.id) as groupsIds, n.federatedIDP as federatedIDP, n.functions as aafFunctions, " +
				"REDUCE(acc=[], pRed IN COLLECT(COALESCE(s.optionEnabled, [])) | pRed+acc ) as optionEnabled";
		final String query2 =
				"MATCH (n:User {id : {id}})-[:IN]->()-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(a:Action)" +
				"<-[:PROVIDE]-(app:Application) " +
				"WHERE HAS(n.login) " +
				"RETURN DISTINCT COLLECT(distinct [a.name,a.displayName,a.type]) as authorizedActions, " +
				"COLLECT(distinct [app.name,app.address,app.icon,app.target,app.displayName,app.display,app.prefix,app.casType,app.scope,app:External]) as apps";
		final String query3 =
				"MATCH (u:User {id: {id}})-[:IN]->(g:Group)-[auth:AUTHORIZED]->(w:Widget) " +
				"WHERE HAS(u.login) " +
				"AND ( NOT(w<-[:HAS_WIDGET]-(:Application)-[:PROVIDE]->(:WorkflowAction)) " +
				"XOR w<-[:HAS_WIDGET]-(:Application)-[:PROVIDE]->(:WorkflowAction)<-[:AUTHORIZE]-(:Role)<-[:AUTHORIZED]-g )  " +
				"OPTIONAL MATCH (w)<-[:HAS_WIDGET]-(app:Application) " +
				"WITH w, app, collect(auth) as authorizations " +
				"RETURN DISTINCT COLLECT({" +
					"id: w.id, name: w.name, " +
					"path: coalesce(app.address, '') + w.path, " +
					"js: coalesce(app.address, '') + w.js, "+
					"i18n: coalesce(app.address, '') + w.i18n, "+
					"application: app.name, " +
					"mandatory: ANY(a IN authorizations WHERE HAS(a.mandatory) AND a.mandatory = true)"+
				"}) as widgets";
		final String query4 = "MATCH (s:Structure) return s.id as id, s.externalId as externalId";
		final String query5 = "MATCH (u:User {id: {id}})-[:PREFERS]->(uac:UserAppConf) RETURN uac AS preferences";
		JsonObject params = new JsonObject();
		params.put("id", userId);
		JsonArray statements = new JsonArray()
				.add(new JsonObject().put("statement", query).put("parameters", params))
				.add(new JsonObject().put("statement", query2).put("parameters", params))
				.add(new JsonObject().put("statement", query3).put("parameters", params))
				.add(new JsonObject().put("statement", query4))
				.add(new JsonObject().put("statement", query5).put("parameters", params));
		neo4j.executeTransaction(statements, null, true, false, true,
				new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray results = message.body().getJsonArray("results");
				if ("ok".equals(message.body().getString("status")) && results != null && results.size() == 5 &&
						results.getJsonArray(0).size() > 0 && results.getJsonArray(1).size() > 0) {
					JsonObject j = results.getJsonArray(0).getJsonObject(0);
					JsonObject j2 = results.getJsonArray(1).getJsonObject(0);
					JsonObject j3 = results.getJsonArray(2).getJsonObject(0);
					JsonObject structureMapping = new JsonObject();
					for (Object o : results.getJsonArray(3)) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject jsonObject = (JsonObject) o;
						structureMapping.put(jsonObject.getString("externalId"), jsonObject.getString("id"));
					}
					j.put("userId", userId);
					JsonObject functions = new JsonObject();
					JsonArray actions = new JsonArray();
					JsonArray apps = new JsonArray();
					for (Object o : getOrElse(j2.getJsonArray("authorizedActions"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						actions.add(new JsonObject()
								.put("name", a.getString(0))
								.put("displayName", a.getString(1))
								.put("type", a.getString(2)));
					}
					for (Object o : getOrElse(j2.getJsonArray("apps"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						apps.add(new JsonObject()
										.put("name", (String) a.getString(0))
										.put("address", (String) a.getString(1))
										.put("icon", (String) a.getString(2))
										.put("target", (String) a.getString(3))
										.put("displayName", (String) a.getString(4))
										.put("display", ((a.getValue(5) == null) || a.getBoolean(5)))
										.put("prefix", (String) a.getString(6))
										.put("casType", (String) a.getString(7))
										.put("scope", (JsonArray) a.getJsonArray(8))
										.put("isExternal", a.getBoolean(9))
						);
					}
					for (Object o : getOrElse(j.getJsonArray("aafFunctions"), new JsonArray())) {
						if (o == null) continue;
						String [] sf = o.toString().split("\\$");
						if (sf.length == 5) {
							JsonObject jo = functions.getJsonObject(sf[1]);
							if (jo == null) {
								jo = new JsonObject().put("code", sf[1])
										.put("functionName", sf[2])
										.put("scope", new JsonArray())
										.put("structureExternalIds", new JsonArray())
										.put("subjects", new JsonObject());
								functions.put(sf[1], jo);
							}
							JsonObject subject = jo.getJsonObject("subjects").getJsonObject(sf[3]);
							if (subject == null) {
								subject = new JsonObject()
										.put("subjectCode", sf[3])
										.put("subjectName", sf[4])
										.put("scope", new JsonArray())
										.put("structureExternalIds", new JsonArray());
								jo.getJsonObject("subjects").put(sf[3], subject);
							}
							jo.getJsonArray("structureExternalIds").add(sf[0]);
							subject.getJsonArray("structureExternalIds").add(sf[0]);
							String sid = structureMapping.getString(sf[0]);
							if (sid != null) {
								jo.getJsonArray("scope").add(sid);
								subject.getJsonArray("scope").add(sid);
							}
						}
					}
					j.remove("aafFunctions");
					for (Object o : getOrElse(j.getJsonArray("functions"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						String code = a.getString(0);
						if (code != null) {
							functions.put(code, new JsonObject()
									.put("code", code)
									.put("scope", a.getJsonArray(1))
							);
						}
					}
					final JsonObject children = new JsonObject();
					final List<String> childrenIds = new ArrayList<String>();
					for (Object o : getOrElse(j.getJsonArray("childrenInfo"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						final JsonArray a = (JsonArray) o;
						final String childId = a.getString(0);
						if (childId != null) {
							childrenIds.add(childId);
							JsonObject jo = children.getJsonObject(childId);
							if (jo == null) {
								jo = new JsonObject()
										.put("lastName", a.getString(1))
										.put("firstName", a.getString(2));
								children.put(childId, jo);
							}
						}
					}
					j.remove("childrenInfo");
					final List<String> classesIds = new ArrayList<String>();
					final List<String> classesNames = new ArrayList<String>() ;
					for (Object o : getOrElse(j.getJsonArray("classes"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						final JsonArray c = (JsonArray) o;
						if (c.getString(0) != null) {
							classesIds.add(c.getString(0));
							classesNames.add(c.getString(1));
						}
					}
					j.remove("classes");
					final List<String> structureIds = new ArrayList<>();
					final List<String> structureNames = new ArrayList<>();
					final Set<String> uai = new HashSet<>();
					boolean hasApp = false;
					boolean attachedToOneStructure = false;
					boolean allAttachedStructuresIgnoreMFA = true;
					for (Object o : getOrElse(j.getJsonArray("structures"), new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						final JsonArray s = (JsonArray) o;
						if (s.getString(0) != null) {
							structureIds.add(s.getString(0));
							structureNames.add(StringUtils.trimToBlank(s.getString(1)));
							if (!StringUtils.isEmpty(s.getString(2))) {
								uai.add(s.getString(2));
							}
							if(!hasApp && getOrElse(s.getBoolean(3), false)) {
								hasApp = true;
							}
							if(allAttachedStructuresIgnoreMFA && Boolean.FALSE.equals(getOrElse(s.getBoolean(4), false))) {
								// This structure does not ignore MFA, so...
								allAttachedStructuresIgnoreMFA = false;
							}
							attachedToOneStructure = true;
						}
					}
					// ignoreMFA is true iif 
					boolean ignoreMFA = attachedToOneStructure && allAttachedStructuresIgnoreMFA;
					j.remove("structures");
					j.put("structures", new JsonArray(structureIds));
					j.put("structureNames", new JsonArray(structureNames));
					j.put("uai", new JsonArray(new ArrayList<>(uai)));
					j.put("hasApp", hasApp);
					j.put("ignoreMFA", ignoreMFA);
					j.put("classes", new JsonArray(classesIds));
					j.put("realClassesNames", new JsonArray(classesNames));
					j.put("functions", functions);
					j.put("authorizedActions", actions);
					j.put("apps", apps);
					j.put("childrenIds", new JsonArray(childrenIds));
					j.put("children", children);
					final JsonObject cache = (results.getJsonArray(4) != null && results.getJsonArray(4).size() > 0 &&
							results.getJsonArray(4).getJsonObject(0) != null) ? results.getJsonArray(4).getJsonObject(0) : new JsonObject();
					j.put("cache", cache);
					j.put("widgets", getOrElse(j3.getJsonArray("widgets"), new JsonArray()));
					//return unique options
					Set<String> uniquOption = new HashSet<>(j.getJsonArray("optionEnabled", new JsonArray()).getList());
					j.put("optionEnabled", new JsonArray(new ArrayList(uniquOption)));
					//
					handler.handle(j);
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
