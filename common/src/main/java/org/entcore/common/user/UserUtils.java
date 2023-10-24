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

package org.entcore.common.user;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import fr.wseduc.webutils.http.Renders;
import static fr.wseduc.webutils.http.Renders.unauthorized;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.JWT;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import fr.wseduc.webutils.security.oauth.OAuthResourceProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static org.entcore.common.http.filter.AppOAuthResourceProvider.getTokenId;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.session.SessionRecreationRequest;
import org.entcore.common.utils.HostUtils;
import org.entcore.common.utils.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserUtils {

	public static final String FIND_SESSION = "findSession";
	public static final String MONITORINGEVENTS = "monitoringevents";
	private static final String USERBOOK_ADDRESS = "userbook.preferences";
	private static final Logger log = LoggerFactory.getLogger(UserUtils.class);
	private static final String COMMUNICATION_USERS = "wse.communication.users";
	private static final String DIRECTORY = "directory";
	public static final String SESSION_ADDRESS = "wse.session";
	private static final JsonArray usersTypes = new JsonArray().add("User");
	private static final JsonObject QUERY_VISIBLE_PROFILS_GROUPS = new JsonObject()
			.put("action", "visibleProfilsGroups");
	private static final JsonObject QUERY_VISIBLE_MANUAL_GROUPS = new JsonObject()
	.put("action", "visibleManualGroups");
	private static final I18n i18n = I18n.getInstance();
	private static final long JWT_TOKEN_EXPIRATION_TIME = 600L;
	private static final long LOG_SESSION_DELAY = 500L;

	private static void findUsers(final EventBus eb, HttpServerRequest request,
								  final JsonObject query, final Handler<JsonArray> handler) {
		getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				if (session != null && session.getString("userId") != null
						&& !session.getString("userId").trim().isEmpty()) {
					findUsers(eb, session.getString("userId"), query, handler);
				} else {
					handler.handle(new JsonArray());
				}
			}
		});
	}

	private static void findUsers(final EventBus eb, String userId,
						  final JsonObject query, final Handler<JsonArray> handler) {
		if (userId != null && !userId.trim().isEmpty()) {
			query.put("userId", userId);
			eb.request(COMMUNICATION_USERS, query, new Handler<AsyncResult<Message<JsonArray>>>() {

				@Override
				public void handle(AsyncResult<Message<JsonArray>> res) {
					if (res.succeeded()) {
						handler.handle(res.result().body());
					} else {
						handler.handle(new JsonArray());
					}
				}
			});
		} else {
			handler.handle(new JsonArray());
		}
	}

	public static void findVisibleUsers(final EventBus eb, HttpServerRequest request, boolean profile,
										final Handler<JsonArray> handler) {
		findVisibleUsers(eb, request, profile, null, null, handler);
	}

	public static void findVisibleUsers(final EventBus eb, HttpServerRequest request, boolean profile,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(customReturn, additionnalParams, false, profile);
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleUsers(final EventBus eb, HttpServerRequest request, boolean itSelf, boolean profile,
			String preFilter, String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(preFilter, customReturn, additionnalParams, itSelf, profile);
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleUsers(final EventBus eb, HttpServerRequest request, boolean itSelf, boolean profile,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(customReturn, additionnalParams, itSelf, profile);
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleUsers(final EventBus eb, String userId, boolean profile,
				final Handler<JsonArray> handler) {
		findVisibleUsers(eb, userId, profile, null, null, handler);
	}

	public static void findVisibleUsers(final EventBus eb, String userId, boolean profile,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(customReturn, additionnalParams, false, profile);
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleUsers(final EventBus eb, String userId, boolean profile, String preFilter,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(preFilter, customReturn, additionnalParams, false, profile);
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleUsers(final EventBus eb, String userId, boolean itSelf, boolean profile,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = queryVisibleUsers(customReturn, additionnalParams, itSelf, profile);
		findUsers(eb, userId, m, handler);
	}

	private static JsonObject queryVisibleUsers(String customReturn, JsonObject additionnalParams, boolean itSelf,
			boolean profile) {
		return queryVisibleUsers(null, customReturn, additionnalParams, itSelf, profile);
	}

	private static JsonObject queryVisibleUsers(String preFilter, String customReturn,
			JsonObject additionnalParams, boolean itSelf, boolean profile) {
		JsonObject m = new JsonObject()
				.put("itself", itSelf)
				.put("profile", profile)
				.put("action", "visibleUsers")
				.put("expectedTypes", usersTypes);
		if (preFilter != null) {
			m.put("preFilter", preFilter);
		}
		if (customReturn != null) {
			m.put("customReturn", customReturn);
		}
		if (additionnalParams != null) {
			m.put("additionnalParams", additionnalParams);
		}
		return m;
	}

	public static void findVisibles(EventBus eb, String userId, String customReturn,
		JsonObject additionnalParams, boolean itSelf, boolean myGroup, boolean profile,
		final Handler<JsonArray> handler) {
		findVisibles(eb, userId, customReturn, additionnalParams, itSelf, myGroup, profile, null, handler);
	}

	public static void findVisibles(EventBus eb, String userId, String customReturn,
		JsonObject additionnalParams, boolean itSelf, boolean myGroup, boolean profile,
		final String acceptLanguage, final Handler<JsonArray> handler) {
		findVisibles(eb, userId, customReturn, additionnalParams, itSelf, myGroup, profile, acceptLanguage, null, handler);
	}

	public static void findVisibles(EventBus eb, String userId, String customReturn,
			JsonObject additionnalParams, boolean itSelf, boolean myGroup, boolean profile,
			final String acceptLanguage, String preFilter, final Handler<JsonArray> handler) {
		JsonObject m = new JsonObject()
				.put("itself", itSelf)
				.put("mygroup", myGroup)
				.put("profile", profile)
				.put("action", "visibleUsers");
		if (preFilter != null) {
			m.put("preFilter", preFilter);
		}
		if (customReturn != null) {
			m.put("customReturn", customReturn);
		}
		if (additionnalParams != null) {
			m.put("additionnalParams", additionnalParams);
		}
		m.put("userId", userId);
		eb.request(COMMUNICATION_USERS, m, new Handler<AsyncResult<Message<JsonArray>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonArray>> res) {
				if (res.succeeded()) {
					JsonArray r = res.result().body();
					if (acceptLanguage != null) {
						translateGroupsNames(r, acceptLanguage);
					}
					handler.handle(r);
				} else {
					log.error("An error occurred while fetching visible users for user " + userId, res.cause());
					handler.handle(new JsonArray());
				}
			}
		});
	}

	public static void translateGroupsNames(JsonArray groups, String acceptLanguage) {
		for (Object u : groups) {
			if (!(u instanceof JsonObject)) continue;
			JsonObject group = (JsonObject) u;
			if (group.getString("name") != null) {
				groupDisplayName(group, acceptLanguage);
			}
		}
	}

	public static void groupDisplayName(JsonObject group, String acceptLanguage) {
		String name = group.getString("name");
		int idx = name.lastIndexOf('-');
		if (idx < 0) { return; }
		final String arg = name.substring(0, idx);
		String type = name.substring(idx + 1);
		String displayName = getOrElse(group.getString("groupDisplayName"), "group." + type);
		String translatedName = i18n.translate(displayName, I18n.DEFAULT_DOMAIN, acceptLanguage, arg);
		if(!translatedName.equals(displayName))
			group.put("name", translatedName);
	}

	public static String groupDisplayName(String name, String groupDisplayName, String acceptLanguage) {
		int idx = name.lastIndexOf('-');
		if (idx < 0) {
			return name;
		}
		String arg = name.substring(0, idx);
		String type = name.substring(idx + 1);
		String displayName = groupDisplayName != null ? groupDisplayName : "group." + type;
		String translatedName = i18n.translate(displayName, I18n.DEFAULT_DOMAIN, acceptLanguage, arg);
		if(!translatedName.equals(displayName)) {
			return translatedName;
		} else {
			return groupDisplayName != null ? groupDisplayName : name;
		}
	}

	public static JsonObject translateAndGroupVisible(JsonArray visibles, String acceptLanguage) {
		return translateAndGroupVisible(visibles, acceptLanguage, false);
	}

	public static JsonObject translateAndGroupVisible(JsonArray visibles, String acceptLanguage, boolean returnGroupType) {
		final JsonObject visible = new JsonObject();
		final JsonArray users = new JsonArray();
		final JsonArray groups = new JsonArray();
		visible.put("groups", groups).put("users", users);
		for (Object o: visibles) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject j = (JsonObject) o;
			if(j.containsKey("positionIds")) {
				formatPositions(j);
			}
			if (j.getString("name") != null) {
				j.remove("positions");
				j.remove("displayName");
				j.remove("profile");
				j.remove("mood");
				if (returnGroupType) {
					Object gt = j.remove("groupType");
					Object gp = j.remove("groupProfile");
					if (gt instanceof Iterable) {
						for (Object gti: (Iterable) gt) {
							if (gti != null && !"Group".equals(gti) && gti.toString().endsWith("Group")) {
								j.put("groupType", gti);
								if ("ProfileGroup".equals(gti)) {
									j.put("profile", gp);
								}
								break;
							}
						}
					}
				}
				j.put("sortName", j.getString("name"));
				UserUtils.groupDisplayName(j, acceptLanguage);
				groups.add(j);
			} else {
				if (returnGroupType) {
					j.remove("groupProfile");
					j.remove("groupType");
				}
				j.remove("name");
				j.remove("nbUsers");
				users.add(j);
			}
		}
		return visible;
	}

	private static void formatPositions(JsonObject dbResult) {
		final JsonArray positionIds = (JsonArray) dbResult.remove("positionIds");
		final JsonArray positionNames = (JsonArray) dbResult.remove("positionNames");
		final JsonArray positions = new JsonArray();
		for(int i = 0; i < positionIds.size(); i++) {
			positions.add(new JsonObject()
				.put("name", positionNames.getString(i))
				.put("id", positionIds.getString(i)));
		}
		dbResult.put("positions", positions);
	}

	public static void findUsersCanSeeMe(final EventBus eb, HttpServerRequest request,
										 final Handler<JsonArray> handler) {
		JsonObject m = new JsonObject()
				.put("action", "usersCanSeeMe");
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, HttpServerRequest request,
												final Handler<JsonArray> handler) {
		findUsers(eb, request, QUERY_VISIBLE_PROFILS_GROUPS, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, HttpServerRequest request,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.put("customReturn", customReturn)
				.put("additionnalParams", additionnalParams);
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.put("customReturn", customReturn)
				.put("additionnalParams", additionnalParams);
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId, String preFilter,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.put("customReturn", customReturn)
				.put("additionnalParams", additionnalParams);
		if (preFilter != null) {
			m.put("preFilter", preFilter);
		}
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId,
												final Handler<JsonArray> handler) {
		findUsers(eb, userId, QUERY_VISIBLE_PROFILS_GROUPS, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId, boolean allowEmptyGroups,
												final Handler<JsonArray> handler) {
		if (allowEmptyGroups) {
			final JsonObject copy = QUERY_VISIBLE_PROFILS_GROUPS.copy();
			final JsonObject additionnalParams = copy.getJsonObject("additionnalParams", new JsonObject());
			copy.put("additionnalParams", additionnalParams.put("excludeEmptyGroups", false));
			findUsers(eb, userId, copy, handler);
		} else {
			findUsers(eb, userId, QUERY_VISIBLE_PROFILS_GROUPS, handler);
		}
	}

	public static void findUsersInProfilsGroups(String groupId, final EventBus eb, String userId,
			boolean itSelf, final Handler<JsonArray> handler) {
		JsonObject m = new JsonObject()
				.put("action", "usersInProfilGroup")
				.put("userId", groupId)
				.put("itself", itSelf)
				.put("excludeUserId", userId);
		eb.request(DIRECTORY, m, new Handler<AsyncResult<Message<JsonArray>>>() {
			@Override
			public void handle(AsyncResult<Message<JsonArray>> res) {
				if (res.succeeded()) {
					handler.handle(res.result().body());
				} else {
					handler.handle(new JsonArray());
				}
			}
		});
	}

	public static void findVisibleManualGroups(final EventBus eb, HttpServerRequest request,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_MANUAL_GROUPS.copy()
				.put("customReturn", customReturn)
				.put("additionnalParams", additionnalParams);
		findUsers(eb, request, m, handler);
	}

	public static void getSession(EventBus eb, final HttpServerRequest request,
								  final Handler<JsonObject> handler) {
		getSession(eb, request, false, handler);
	}

	public static Optional<String> getSessionId(final HttpServerRequest request){
		final String oneSessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		if (oneSessionId != null && !oneSessionId.trim().isEmpty()) {
			return Optional.ofNullable(oneSessionId);
		}else{
			return Optional.empty();
		}
	}

	public static void getSession(EventBus eb, final HttpServerRequest request, boolean paused,
								  final Handler<JsonObject> handler) {
		if (request instanceof SecureHttpServerRequest &&
				((SecureHttpServerRequest) request).getSession() != null) {
			handler.handle(((SecureHttpServerRequest) request).getSession());
		} else {
			final String oneSessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
			Promise<String> promise = Promise.promise();
			if (request instanceof SecureHttpServerRequest) {
				promise.complete( ((SecureHttpServerRequest) request).getAttribute("remote_user") );
			} else {
				promise.complete( null );
			}
			promise.future()
			.compose( remoteUserId -> {
				// If request attributes are not set yet, and if a bearer token exists,
				// try retrieving the remote_user by validating the token.
				if( StringUtils.isEmpty(oneSessionId) && remoteUserId==null) {
					final SecureHttpServerRequest secureRequest = (request instanceof SecureHttpServerRequest) ? (SecureHttpServerRequest) request : new SecureHttpServerRequest(request);
					OAuthResourceProvider provider = new DefaultOAuthResourceProvider(eb);
					Promise<String> attrPromise = Promise.promise();
					if( provider.hasBearerHeader(secureRequest) ) {
						request.pause();

						provider.validToken(secureRequest, r -> {
							if (paused) request.pause();  // provider.validToken() may have resumed the request, so pause it again.
							else 		request.resume(); // Request was not paused, so resume it.
							attrPromise.complete( secureRequest.getAttribute("remote_user") );
						});
					} else {
						attrPromise.complete( (String) null );
					}
					return attrPromise.future();
				} else {
					return Future.succeededFuture(remoteUserId);
				}
			})
			.onSuccess( remoteUserId -> {
				final String sessionId;
				// The session id can come whether from the cookie oneSessionId for web users
				// or from the access token in the Authorization header of the request #WB-1578
				final boolean oAuthIdentified;
				if(oneSessionId != null && !oneSessionId.trim().isEmpty()) {
					sessionId = oneSessionId;
					oAuthIdentified = false;
				} else {
					final Optional<String> maybeTokenId = getTokenId(request);
					if(maybeTokenId.isPresent()) {
						sessionId = maybeTokenId.get();
						oAuthIdentified = true;
					} else {
						sessionId = null;
						oAuthIdentified = false;
					}
				}
				if (sessionId == null && isEmpty(remoteUserId)) {
					log.debug("A request came for user " + remoteUserId + " with no way to get its session");
					handler.handle(null);
				} else {
					if (!paused) {
						request.pause();
					}
					findOrRecreateSession(remoteUserId, sessionId, oAuthIdentified, eb, request , paused)
					.onComplete(e -> {
						if(e.succeeded()) {
							handler.handle(e.result());
						} else {
							handler.handle(null);
						}
					});
				}
			});
		}
	}

	/**
	 * Try to fetch a session based on the supplied {@code sessionId}.
	 * If the session doesn't exist and the user is oauth authenticated then we try to
	 * recreate a session and then fetch again the newly created session.
	 * But if the session doesn't exist and the user IS NOT oauth authenticated, we just
	 * send back {@code null}.
	 * @param userId Id of the authenticated user
	 * @param sessionId SessionId of the user
	 * @param oAuthIdentified {@code true} if the user is authenticated via oauth
	 * @param eb Event bus to use to communicate with AuthManager
	 * @param request Http request of the user that needs a session
	 * @param paused {@code true} if the request has been paused
	 * @return The session that has been found (or created) or {@code null} if no session
	 * could be found (or recreated)
	 */
	private static Future<JsonObject> findOrRecreateSession(final String userId,
											  final String sessionId,
											  final boolean oAuthIdentified,
											  final EventBus eb,
											  final HttpServerRequest request,
											  final boolean paused) {
		final Promise<JsonObject> sessionPromise = Promise.promise();
		final JsonObject findSession = new JsonObject()
				.put("action", "find")
				.put("sessionId", sessionId);
		findSession(eb, request, findSession, paused, findSessionResult -> {
			if(findSessionResult == null) {
				if(oAuthIdentified) {
					log.debug("[GetSession] Could not find a session for our user who has a valid token -> we will recreate one");
					reCreateSession(eb, userId, request, paused).onSuccess(recreationResult -> {
						log.debug("[GetSession] A session has been recreated for oauth authenticated user " + userId);
						sessionPromise.complete(recreationResult);
					}).onFailure(err -> {
						log.warn("[GetSession] Could not recreate " + userId + "'s session : ", err);
						sessionPromise.fail(err);
					});
				} else {
					log.debug("[GetSession] Could not find a session for our user " + userId);
					sessionPromise.fail("session.not.found");
				}
			} else {
				log.debug("[GetSession] A session has been found for user " + userId);
				sessionPromise.complete(findSessionResult);
			}
		});
		return sessionPromise.future();
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession,
									final Handler<JsonObject> handler) {
		findSession(eb, request, findSession, false, handler);
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession, final boolean paused,
			final Handler<JsonObject> handler) {
		final long startSessionTime = System.currentTimeMillis();
		eb.request(SESSION_ADDRESS, findSession, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> message) {
				if (message.succeeded()) {
					JsonObject body = message.result().body();
					JsonObject session = message.result().body().getJsonObject("session");
					if (request != null && !paused) {
						request.resume();
					}
					if ("ok".equals(message.result().body().getString("status")) && session != null) {
						if (request instanceof SecureHttpServerRequest) {
							((SecureHttpServerRequest) request).setSession(session);
						}
						handler.handle(session);
					} else {
						handler.handle(null);
						if (log.isDebugEnabled()) {
							final String key = findSession.getString("sessionId", "user="+findSession.getString("userId"));
							log.debug("Could not found session: "+ key + " error: " + body.getString("error") + " message: " + body.getString("message"));
						}
					}
				} else {
					handler.handle(null);
					final String key = findSession.getString("sessionId", "user="+findSession.getString("userId"));
					log.error("Could not found session: " + key + " cause: " + message.cause());
				}
				findSessionMonitoring(startSessionTime, message);
			}
		});
	}

	private static void findSessionMonitoring(long startSessionTime, AsyncResult<Message<JsonObject>> message) {
		final long timeGetSessionDelay = System.currentTimeMillis() - startSessionTime;
		if (timeGetSessionDelay > LOG_SESSION_DELAY) {
			log.info("Find session time : " + timeGetSessionDelay + " ms.");
			final MongoDb mongoDb = MongoDb.getInstance();
			if (mongoDb.isInitialized()) {
				final JsonObject sessionMonitoring = new JsonObject()
						.put("date", MongoDb.now())
						.put("epoch", startSessionTime)
						.put("type", FIND_SESSION)
						.put("hostname", HostUtils.getHostName())
						.put("delay", timeGetSessionDelay)
						.put("ar-succeeded", message.succeeded());
				if (message.succeeded()) {
					sessionMonitoring.put("status-ok", "ok".equals(message.result().body().getString("status")));
				}
				mongoDb.save(MONITORINGEVENTS, sessionMonitoring);
			}
		}
	}

	public static void getSessionByUserId(EventBus eb, final String userId, final Handler<JsonObject> handler) {
		JsonObject findSession = new JsonObject()
				.put("action", "findByUserId")
				.put("userId", userId)
				.put("allowDisconnectedUser", true);
		findSession(eb, null, findSession, handler);
	}

	public static void getSession(EventBus eb, final String sessionId,  final Handler<JsonObject> handler) {
		JsonObject findSession = new JsonObject()
				.put("action", "find")
				.put("sessionId", sessionId);
		findSession(eb, null, findSession, handler);
	}

	public static UserInfos sessionToUserInfos(JsonObject session) {
		if (session == null) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(session.encode(), UserInfos.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void getUserInfos(EventBus eb, HttpServerRequest request,
									final Handler<UserInfos> handler) {
		getSession(eb, request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				handler.handle(sessionToUserInfos(session));
			}
		});
	}

	public static Future<UserInfos> getAuthenticatedUserInfos(EventBus eb, HttpServerRequest request) {
		final Promise<UserInfos> promise = Promise.promise();
		getSession(eb, request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				final UserInfos userInfo = sessionToUserInfos(session);
				if(userInfo == null) {
					unauthorized(request);
					promise.fail("user.not.found");
				} else {
					promise.complete(userInfo);
				}
			}
		});
		return promise.future();
	}

	public static void getUserInfos(EventBus eb, String userId,
									final Handler<UserInfos> handler) {
		getSessionByUserId(eb, userId, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				handler.handle(sessionToUserInfos(session));
			}
		});
	}

	/**
	 * Re-create a session for a user
	 * @param eb Event bus to communicate with auth-manager
	 * @param userId Id of the user who needs a new session
	 * @param request Http request that generated the need of a new session
	 * @param paused {@code true} if the session has already been paused => resume is supposed to happen somewhere else
	 * @return The re-created session. Can be null if the user is OAuthSystemUser
	 */
	public static Future<JsonObject> reCreateSession(final EventBus eb,
													 final String userId,
													 final HttpServerRequest request,
													 final boolean paused) {
		final Promise<JsonObject> details = Promise.promise();
		final boolean isOAuthRequest = request instanceof SecureHttpServerRequest && getTokenId((SecureHttpServerRequest) request).isPresent();
		final SessionRecreationRequest recreationRequest = new SessionRecreationRequest(userId, getSessionIdOrTokenId(request).orElse(null), isOAuthRequest);
		if(request != null && !paused) {
			request.pause();
		}
		eb.request(SESSION_ADDRESS, JsonObject.mapFrom(recreationRequest), new Handler<AsyncResult<Message<JsonObject>>>() {
			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if(request != null && !paused) {
					request.resume();
				}
				if (res.succeeded()) {
					details.complete( res.result().body() ); // body may be null if no session can be created (for an app)
				} else {
					details.fail(res.cause());
				}
			}
		});
		return details.future();
	}
	public static Future<String> createSessionWithId(final EventBus eb, final String userId,
										   final String desiredSessionId,
										   final boolean secureLocation) {
		final Promise<String> promise = Promise.promise();
		createSession(eb, userId, desiredSessionId, null, null, secureLocation, sessionId -> promise.complete(sessionId));
		return promise.future();
	}
	public static void createSession(EventBus eb, String userId, boolean secureLocation, Handler<String> handler) {
		createSession(eb, userId, null, null, null, secureLocation, handler);
	}

	public static void createSession(EventBus eb, String userId, String sessionIndex, String nameId, Handler<String> handler) {
		createSession(eb, userId, null, sessionIndex, nameId, false, handler);
	}

	public static void createSession(EventBus eb, String userId, final String desiredSessionId,
									 String sessionIndex, String nameId,
			boolean secureLocation, final Handler<String> handler) {
		final JsonObject json = new JsonObject()
				.put("action", "create")
				.put("userId", userId);
		if (sessionIndex != null && nameId != null && !sessionIndex.trim().isEmpty() && !nameId.trim().isEmpty()) {
			json.put("SessionIndex", sessionIndex).put("NameID", nameId);
		}
		if (secureLocation) {
			json.put("secureLocation", secureLocation);
		}
		if(desiredSessionId != null && !desiredSessionId.isEmpty()) {
			json.put("sessionId", desiredSessionId);
		}
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					if (res.succeeded() && "ok".equals(res.result().body().getString("status"))) {
						handler.handle(res.result().body().getString("sessionId"));
					} else {
						handler.handle(null);
					}
				}
			}
		});
	}

	public static void deleteSession(EventBus eb, String sessionId,
									 final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "drop")
				.put("sessionId", sessionId);
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					handler.handle(res.succeeded() && "ok".equals(res.result().body().getString("status")));
				}
			}
		});
	}

	public static void deleteSessionWithMetadata(EventBus eb, String sessionId,
			final Handler<JsonObject> handler) {
		JsonObject json = new JsonObject()
				.put("action", "drop")
				.put("sessionMetadata", true)
				.put("sessionId", sessionId);
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					if (res.succeeded() && "ok".equals(res.result().body().getString("status"))) {
						handler.handle(res.result().body().getJsonObject("sessionMetadata"));
					} else {
						handler.handle(null);
					}
				}
			}
		});
	}

	public static void deletePermanentSession(EventBus eb, String userId, String currentSessionId, String currentTokenId,
			final Handler<Boolean> handler) {
		deletePermanentSession(eb, userId, currentSessionId, currentTokenId, true, handler);
	}

	public static void deletePermanentSession(EventBus eb, String userId, String currentSessionId, String currentTokenId,
											  boolean immediate, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "dropPermanentSessions")
				.put("userId", userId)
				.put("currentSessionId", currentSessionId)
				.put("currentTokenId", currentTokenId)
				.put("immediate", immediate);
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					handler.handle(res.succeeded() && "ok".equals(res.result().body().getString("status")));
				}
			}
		});
	}

	public static void deleteCacheSession(EventBus eb, String userId, String currentSessionId, final Handler<JsonArray> droppedSessionsHandler) {
		JsonObject json = new JsonObject()
				.put("action", "dropCacheSession")
				.put("currentSessionId", currentSessionId)
				.put("userId", userId);
		eb.request(SESSION_ADDRESS, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (droppedSessionsHandler != null) {
					if("ok".equals(res.body().getString("status")))
						droppedSessionsHandler.handle(res.body().getJsonArray("dropped"));
					else
						droppedSessionsHandler.handle(null);
				}
			}
		}));
	}

	public static void addSessionAttributeOnId(EventBus eb, String sessionId,
										   String key, String value, final Handler<Boolean> handler) {
		addSessionAttributeOnId(eb, sessionId, key, (Object) value, handler);
	}

	public static void addSessionAttribute(EventBus eb, String userId,
	String key, String value, final Handler<Boolean> handler) {
		addSessionAttribute(eb, userId, key, (Object) value, handler);
	}

	public static void addSessionAttribute(EventBus eb, String userId,
	String key, Long value, final Handler<Boolean> handler) {
		addSessionAttribute(eb, userId, key, (Object) value, handler);
	}

	@Deprecated
	public static void addSessionAttribute(EventBus eb, String userId,
	String key, JsonObject value, final Handler<Boolean> handler) {
		addSessionAttribute(eb, userId, key, (Object) value, handler);
	}

	private static void addSessionAttributeOnId(EventBus eb, String sessionId,
											String key, Object value, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "addAttributeOnSessionId")
				.put("sessionId", sessionId)
				.put("key", key)
				.put("value", value);

		sendSessionAttribute(eb, handler, json);
	}

	private static void addSessionAttribute(EventBus eb, String userId,
			String key, Object value, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "addAttribute")
				.put("userId", userId)
				.put("key", key)
				.put("value", value);

		sendSessionAttribute(eb, handler, json);
	}

	private static void sendSessionAttribute(EventBus eb, final Handler<Boolean> handler, JsonObject json) {
		final long startAddAttrSessionTime = System.currentTimeMillis();
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					handler.handle(res.succeeded() && "ok".equals(res.result().body().getString("status")));
				}
				final long timeAddAttrSessionDelay = System.currentTimeMillis() - startAddAttrSessionTime;
				if (timeAddAttrSessionDelay > LOG_SESSION_DELAY) {
					log.info("Add session attribute time : " + timeAddAttrSessionDelay + " ms.");
				}
			}
		});
	}

	public static void removeSessionAttribute(EventBus eb, String userId,
			String key, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "removeAttribute")
				.put("userId", userId)
				.put("key", key);
		eb.request(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					handler.handle(res.succeeded() && "ok".equals(res.result().body().getString("status")));
				}
			}
		});
	}

	public static String createJWTToken(Vertx vertx, UserInfos user, String clientId, HttpServerRequest request) throws Exception {
		final JWT jwt = new JWT(vertx, (String) vertx.sharedData().getLocalMap("server").get("signKey"), null);
		final JsonObject payload = createJWTClaim(
			user.getUserId(), clientId, JWT_TOKEN_EXPIRATION_TIME,
			(request != null) ? Renders.getHost(request) : null
		);
		return jwt.encodeAndSignHmac(payload);
	}

	/**
	 * Create a new JWT intended to be used in HTTP query params.
	 * - payload is signed, proving the server was the emitter,
	 * - is has a short time-to-live ;
	 * @param vertx
	 * @param userId id of the (session) user
	 * @param clientId 
	 * @param ttlInSeconds Generated token will be valid until this amount of seconds has elapsed
	 * @param request null, or used to fill the issuer claim in the token
	 * @return a signed JWT
	 * @throws Exception
	 */
	public static String createJWTForQueryParam(
				Vertx vertx, String userId, String clientId, long ttlInSeconds, HttpServerRequest request
			) throws Exception {
		final JWT jwt = new JWT(vertx, (String) vertx.sharedData().getLocalMap("server").get("signKey"), null);
		final JsonObject payload = createJWTClaim(userId, clientId,
			(0>=ttlInSeconds || ttlInSeconds>JWT_TOKEN_EXPIRATION_TIME) ? JWT_TOKEN_EXPIRATION_TIME : ttlInSeconds,
			(request != null) ? Renders.getHost(request) : null
		);
		return jwt.encodeAndSignHmac(payload);
	}

	private static JsonObject createJWTClaim(String userId, String intendedAudience, long ttlInSeconds, String issuer) {
		final JsonObject payload = new JsonObject();
		final long issuedAt = System.currentTimeMillis() / 1000;
		if (issuer != null) payload.put("iss", issuer);
		payload.put("aud", intendedAudience)
			   .put("sub", userId)
			   .put("iat", issuedAt)
			   .put("exp", issuedAt + ttlInSeconds);
		return payload;
	}

	public static void getUserIdsForGroupIds(Set<String> groupsIds, String currentUserId, EventBus eb, Handler<AsyncResult<Set<String>>> h) {
		final List<Future> futures = groupsIds.stream().map((groupId) -> {
			Promise<Set<String>> future = Promise.promise();
			UserUtils.findUsersInProfilsGroups(groupId, eb, currentUserId, false, (ev) -> {
				Set<String> ids = new HashSet<>();
				if (ev != null) {

          for (Object o : ev) {
            if (o instanceof JsonObject) {
              JsonObject j = (JsonObject) o;
              String id = j.getString("id");
              ids.add(id);
            }
          }
				}

				future.complete(ids);
			});
			return future.future();
		}).collect(Collectors.toList());
		CompositeFuture.all(futures).map((result) -> {
			List<Set<String>> all = result.list();
			return all.stream().reduce(new HashSet<>(), (a1, a2) -> {
				a1.addAll(a2);
				return a1;
			});
		}).onComplete(h);
	}

	public static boolean isSuperAdmin(UserInfos user) {
		Map<String, UserInfos.Function> functions = user.getFunctions();
		if (functions == null || functions.isEmpty()) {
			return false;
		}
		return functions.containsKey(DefaultFunctions.SUPER_ADMIN);
	}

	public static void getSessionsNumber(EventBus eb, final Handler<AsyncResult<Long>> handler) {
		final JsonObject json = new JsonObject().put("action", "sessionNumber");
		eb.request(SESSION_ADDRESS, json, ar -> {
			if (ar.succeeded()) {
				handler.handle(Future.succeededFuture(((JsonObject) ar.result().body()).getLong("count")));
			} else {
				handler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	public static void getTheme(EventBus eb, HttpServerRequest request, Map<String, String> hostSkin, final Handler<String> handler) {
		if (request instanceof SecureHttpServerRequest) {
			JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if (session != null) {
				JsonObject cache = session.getJsonObject("cache");
				if (cache != null) {
					JsonObject preferences = cache.getJsonObject("preferences");
					if (preferences != null) {
						String theme = preferences.getString("theme");
						if (Utils.isNotEmpty(theme)) {
							handler.handle(theme);
							return;
						}
					}
				}
			}
		}

		UserUtils.getUserInfos(eb, request, userInfos -> {
			String query = "MATCH (u: User {id: {userId}})-[:PREFERS]->(uac: UserAppConf) RETURN uac.theme as theme";
			Map<String, Object> params = new HashMap<>();
			params.put("userId", userInfos.getUserId());

			Neo4j.getInstance().execute(query, params, event -> {
				if ("ok".equals(event.body().getString("status"))) {
					JsonArray result = event.body().getJsonArray("result");
					if (result != null && result.size() == 1) {
						String theme = result.getJsonObject(0).getString("theme");
						if (isNotEmpty(theme)) {
							handler.handle(theme);
							return;
						}
					}
				}

				String themeFromCookie = CookieHelper.get("theme", request);
				if (isNotEmpty(themeFromCookie)) {
					handler.handle(themeFromCookie);
					return;
				}

				String themeFromHost = hostSkin.get(Renders.getHost(request));
				if (isNotEmpty(themeFromHost)) {
					handler.handle(themeFromHost);
					return;
				}
			});
		});
	}

	public static  Future<JsonObject> getUserPreferences(final EventBus eb, final HttpServerRequest request, final String application) {
		final Promise<JsonObject> promise = Promise.promise();
		final JsonObject params = new JsonObject().put("action", "get.currentuser")
				.put("request", new JsonObject().put("headers", new JsonObject().put("Cookie", request.getHeader("Cookie"))))
				.put("application", "theme");
		eb.request(USERBOOK_ADDRESS, params, (final AsyncResult<Message<JsonObject>> event) -> {
			if (event.succeeded()) {
				final JsonObject body = event.result().body();
				if ("error".equals(body.getString("status"))) {
					promise.fail(body.getString("error"));
				} else {
					promise.complete(body.getJsonObject("value", new JsonObject()));
				}
			} else {
				promise.fail(event.cause());
			}
		});
		return promise.future();
	}
	public static Optional<String> getSessionIdOrTokenId(final HttpServerRequest request) {
		final String oneSessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		if(StringUtils.isEmpty(oneSessionId)) {
			if (request instanceof SecureHttpServerRequest) {
				return getTokenId((SecureHttpServerRequest) request);
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.of(oneSessionId);
		}
	}

	public static enum ErrorCodes {
		SESSION_NOT_CREATED("session.creation.error");

		private final String code;

		ErrorCodes(final String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

}

