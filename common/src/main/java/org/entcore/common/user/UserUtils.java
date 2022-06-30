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

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.JWT;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import fr.wseduc.webutils.security.oauth.OAuthResourceProvider;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.neo4j.Neo4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.*;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class UserUtils {

	private static final Logger log = LoggerFactory.getLogger(UserUtils.class);
	private static final String COMMUNICATION_USERS = "wse.communication.users";
	private static final String DIRECTORY = "directory";
	private static final String SESSION_ADDRESS = "wse.session";
	private static final JsonArray usersTypes = new fr.wseduc.webutils.collections.JsonArray().add("User");
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
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
				}
			}
		});
	}

	private static void findUsers(final EventBus eb, String userId,
						  final JsonObject query, final Handler<JsonArray> handler) {
		if (userId != null && !userId.trim().isEmpty()) {
			query.put("userId", userId);
			eb.send(COMMUNICATION_USERS, query, new Handler<AsyncResult<Message<JsonArray>>>() {

				@Override
				public void handle(AsyncResult<Message<JsonArray>> res) {
					if (res.succeeded()) {
						handler.handle(res.result().body());
					} else {
						handler.handle(new fr.wseduc.webutils.collections.JsonArray());
					}
				}
			});
		} else {
			handler.handle(new fr.wseduc.webutils.collections.JsonArray());
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
		eb.send(COMMUNICATION_USERS, m, new Handler<AsyncResult<Message<JsonArray>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonArray>> res) {
				if (res.succeeded()) {
					JsonArray r = res.result().body();
					if (acceptLanguage != null) {
						translateGroupsNames(r, acceptLanguage);
					}
					handler.handle(r);
				} else {
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
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
		final JsonArray users = new fr.wseduc.webutils.collections.JsonArray();
		final JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
		visible.put("groups", groups).put("users", users);
		for (Object o: visibles) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject j = (JsonObject) o;
			if (j.getString("name") != null) {
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
		eb.send(DIRECTORY, m, new Handler<AsyncResult<Message<JsonArray>>>() {
			@Override
			public void handle(AsyncResult<Message<JsonArray>> res) {
				if (res.succeeded()) {
					handler.handle(res.result().body());
				} else {
					handler.handle(new fr.wseduc.webutils.collections.JsonArray());
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
				if( (oneSessionId==null || oneSessionId.trim().isEmpty()) && remoteUserId==null && request instanceof SecureHttpServerRequest) {
					final SecureHttpServerRequest secureRequest = (SecureHttpServerRequest) request;
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
				if ((oneSessionId == null || oneSessionId.trim().isEmpty()) &&
						(remoteUserId == null || remoteUserId.trim().isEmpty())) {
					handler.handle(null);
				} else {
					if (!paused) {
						request.pause();
					}
					JsonObject findSession = new JsonObject();
					if (oneSessionId != null && !oneSessionId.trim().isEmpty()) {
						findSession.put("action", "find")
								.put("sessionId", oneSessionId);
					} else { // remote user (oauth)
						findSession.put("action", "findByUserId")
								.put("userId", remoteUserId)
								.put("allowDisconnectedUser", true);
					}
					findSession(eb, request, findSession, paused, handler);
				}
			});
		}
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession,
									final Handler<JsonObject> handler) {
		findSession(eb, request, findSession, false, handler);
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession, final boolean paused,
			final Handler<JsonObject> handler) {
		final long startSessionTime = System.currentTimeMillis();
		eb.send(SESSION_ADDRESS, findSession, new Handler<AsyncResult<Message<JsonObject>>>() {

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
						final String key = findSession.getString("sessionId", "user="+findSession.getString("userId"));
						log.warn("Could not found session: "+ key + " error: " + body.getString("error") + " message: " + body.getString("message"));
					}
				} else {
					handler.handle(null);
					final String key = findSession.getString("sessionId", "user="+findSession.getString("userId"));
					log.error("Could not found session: " + key + " cause: " + message.cause());
				}
				final long timeGetSessionDelay = System.currentTimeMillis() - startSessionTime;
				if (timeGetSessionDelay > LOG_SESSION_DELAY) {
					log.info("Find session time : " + timeGetSessionDelay + " ms.");
				}
			}
		});
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

	public static void getUserInfos(EventBus eb, String userId,
									final Handler<UserInfos> handler) {
		getSessionByUserId(eb, userId, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				handler.handle(sessionToUserInfos(session));
			}
		});
	}

	public static void createSession(EventBus eb, String userId, boolean secureLocation, Handler<String> handler) {
		createSession(eb, userId, null, null, secureLocation, handler);
	}

	public static void createSession(EventBus eb, String userId, String sessionIndex, String nameId, Handler<String> handler) {
		createSession(eb, userId, sessionIndex, nameId, false, handler);
	}

	public static void createSession(EventBus eb, String userId, String sessionIndex, String nameId,
			boolean secureLocation, final Handler<String> handler) {
		JsonObject json = new JsonObject()
				.put("action", "create")
				.put("userId", userId);
		if (sessionIndex != null && nameId != null && !sessionIndex.trim().isEmpty() && !nameId.trim().isEmpty()) {
			json.put("SessionIndex", sessionIndex).put("NameID", nameId);
		}
		if (secureLocation) {
			json.put("secureLocation", secureLocation);
		}
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

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
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

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
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

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
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (handler != null) {
					handler.handle(res.succeeded() && "ok".equals(res.result().body().getString("status")));
				}
			}
		});
	}

	public static void deleteCacheSession(EventBus eb, String userId, String currentSessionId, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.put("action", "dropCacheSession")
				.put("currentSessionId", currentSessionId)
				.put("userId", userId);
		eb.send(SESSION_ADDRESS, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					handler.handle("ok".equals(res.body().getString("status")));
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
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

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
		eb.send(SESSION_ADDRESS, json, new Handler<AsyncResult<Message<JsonObject>>>() {

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
		List<Future> futures = (List)groupsIds.stream().map((groupId) -> {
			Future<Set<String>> future = Future.future();
			UserUtils.findUsersInProfilsGroups(groupId, eb, currentUserId, false, (ev) -> {
				Set<String> ids = new HashSet();
				if (ev != null) {
					Iterator var3 = ev.iterator();

					while(var3.hasNext()) {
						Object o = var3.next();
						if (o instanceof JsonObject) {
							JsonObject j = (JsonObject)o;
							String id = j.getString("id");
							ids.add(id);
						}
					}
				}

				future.complete(ids);
			});
			return future;
		}).collect(Collectors.toList());
		CompositeFuture.all(futures).map((result) -> {
			List<Set<String>> all = result.list();
			return (Set)all.stream().reduce(new HashSet(), (a1, a2) -> {
				a1.addAll(a2);
				return a1;
			});
		}).map(e->{
			return (Set<String>)e;
		}).setHandler(h);
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
		eb.send(SESSION_ADDRESS, json, ar -> {
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

}

