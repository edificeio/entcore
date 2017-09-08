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

package org.entcore.common.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;

public class UserUtils {

	private static final String COMMUNICATION_USERS = "wse.communication.users";
	private static final String DIRECTORY = "directory";
	private static final String SESSION_ADDRESS = "wse.session";
	private static final JsonArray usersTypes = new JsonArray().addString("User");
	private static final JsonObject QUERY_VISIBLE_PROFILS_GROUPS = new JsonObject()
			.putString("action", "visibleProfilsGroups");
	private static final JsonObject QUERY_VISIBLE_MANUAL_GROUPS = new JsonObject()
	.putString("action", "visibleManualGroups");
	private static final I18n i18n = I18n.getInstance();

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
			query.putString("userId", userId);
			eb.send(COMMUNICATION_USERS, query, new Handler<Message<JsonArray>>() {

				@Override
				public void handle(Message<JsonArray> res) {
					handler.handle(res.body());
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
				.putBoolean("itself", itSelf)
				.putBoolean("profile", profile)
				.putString("action", "visibleUsers")
				.putArray("expectedTypes", usersTypes);
		if (preFilter != null) {
			m.putString("preFilter", preFilter);
		}
		if (customReturn != null) {
			m.putString("customReturn", customReturn);
		}
		if (additionnalParams != null) {
			m.putObject("additionnalParams", additionnalParams);
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
				.putBoolean("itself", itSelf)
				.putBoolean("mygroup", myGroup)
				.putBoolean("profile", profile)
				.putString("action", "visibleUsers");
		if (preFilter != null) {
			m.putString("preFilter", preFilter);
		}
		if (customReturn != null) {
			m.putString("customReturn", customReturn);
		}
		if (additionnalParams != null) {
			m.putObject("additionnalParams", additionnalParams);
		}
		m.putString("userId", userId);
		eb.send(COMMUNICATION_USERS, m, new Handler<Message<JsonArray>>() {

			@Override
			public void handle(Message<JsonArray> res) {
				JsonArray r = res.body();
				if (acceptLanguage != null) {
					translateGroupsNames(r, acceptLanguage);
				}
				handler.handle(r);
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
		if (name == null || idx < 0) { return; }
		String arg = name.substring(0, idx);
		String type = name.substring(idx + 1);
		String displayName = group.getString("groupDisplayName", "group." + type);
		String translatedName = i18n.translate(displayName, I18n.DEFAULT_DOMAIN, acceptLanguage, arg);
		if(!translatedName.equals(displayName))
			group.putString("name", translatedName);
	}

	public static String groupDisplayName(String name, String groupDisplayName, String acceptLanguage) {
		int idx = name.lastIndexOf('-');
		if (name == null || idx < 0) {
			return name;
		}
		String arg = name.substring(0, idx);
		String type = name.substring(idx + 1);
		String displayName = groupDisplayName != null ? groupDisplayName : "group." + type;
		return i18n.translate(displayName, I18n.DEFAULT_DOMAIN, acceptLanguage, arg);
	}

	public static void findUsersCanSeeMe(final EventBus eb, HttpServerRequest request,
										 final Handler<JsonArray> handler) {
		JsonObject m = new JsonObject()
				.putString("action", "usersCanSeeMe");
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, HttpServerRequest request,
												final Handler<JsonArray> handler) {
		findUsers(eb, request, QUERY_VISIBLE_PROFILS_GROUPS, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, HttpServerRequest request,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.putString("customReturn", customReturn)
				.putObject("additionnalParams", additionnalParams);
		findUsers(eb, request, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.putString("customReturn", customReturn)
				.putObject("additionnalParams", additionnalParams);
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId, String preFilter,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_PROFILS_GROUPS.copy()
				.putString("customReturn", customReturn)
				.putObject("additionnalParams", additionnalParams);
		if (preFilter != null) {
			m.putString("preFilter", preFilter);
		}
		findUsers(eb, userId, m, handler);
	}

	public static void findVisibleProfilsGroups(final EventBus eb, String userId,
												final Handler<JsonArray> handler) {
		findUsers(eb, userId, QUERY_VISIBLE_PROFILS_GROUPS, handler);
	}

	public static void findUsersInProfilsGroups(String groupId, final EventBus eb, String userId,
			boolean itSelf, final Handler<JsonArray> handler) {
		JsonObject m = new JsonObject()
				.putString("action", "usersInProfilGroup")
				.putString("userId", groupId)
				.putBoolean("itself", itSelf)
				.putString("excludeUserId", userId);
		eb.send(DIRECTORY, m, new Handler<Message<JsonArray>>() {
			@Override
			public void handle(Message<JsonArray> res) {
				handler.handle(res.body());
			}
		});
	}

	public static void findVisibleManualGroups(final EventBus eb, HttpServerRequest request,
			String customReturn, JsonObject additionnalParams, final Handler<JsonArray> handler) {
		JsonObject m = QUERY_VISIBLE_MANUAL_GROUPS.copy()
				.putString("customReturn", customReturn)
				.putObject("additionnalParams", additionnalParams);
		findUsers(eb, request, m, handler);
	}

	public static void getSession(EventBus eb, final HttpServerRequest request,
								  final Handler<JsonObject> handler) {
		getSession(eb, request, false, handler);
	}

	public static void getSession(EventBus eb, final HttpServerRequest request, boolean paused,
								  final Handler<JsonObject> handler) {
		if (request instanceof SecureHttpServerRequest &&
				((SecureHttpServerRequest) request).getSession() != null) {
			handler.handle(((SecureHttpServerRequest) request).getSession());
		} else {
			String oneSessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
			String remoteUserId = null;
			if (request instanceof SecureHttpServerRequest) {
				remoteUserId = ((SecureHttpServerRequest) request).getAttribute("remote_user");
			}
			if ((oneSessionId == null || oneSessionId.trim().isEmpty()) &&
					(remoteUserId == null || remoteUserId.trim().isEmpty())) {
				handler.handle(null);
				return;
			} else {
				if (!paused) {
					request.pause();
				}
				JsonObject findSession = new JsonObject();
				if (oneSessionId != null && !oneSessionId.trim().isEmpty()) {
					findSession.putString("action", "find")
							.putString("sessionId", oneSessionId);
				} else { // remote user (oauth)
					findSession.putString("action", "findByUserId")
							.putString("userId", remoteUserId)
							.putBoolean("allowDisconnectedUser", true);
				}
				findSession(eb, request, findSession, paused, handler);
			}
		}
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession,
									final Handler<JsonObject> handler) {
		findSession(eb, request, findSession, false, handler);
	}

	private static void findSession(EventBus eb, final HttpServerRequest request, JsonObject findSession, final boolean paused,
			final Handler<JsonObject> handler) {
		eb.send(SESSION_ADDRESS, findSession, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonObject session = message.body().getObject("session");
				if (request != null && !paused) {
					request.resume();
				}
				if ("ok".equals(message.body().getString("status")) && session != null) {
					if (request instanceof SecureHttpServerRequest) {
						((SecureHttpServerRequest) request).setSession(session);
					}
					handler.handle(session);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	public static void getSessionByUserId(EventBus eb, final String userId, final Handler<JsonObject> handler) {
		JsonObject findSession = new JsonObject()
				.putString("action", "findByUserId")
				.putString("userId", userId)
				.putBoolean("allowDisconnectedUser", true);
		findSession(eb, null, findSession, handler);
	}

	public static void getSession(EventBus eb, final String sessionId,  final Handler<JsonObject> handler) {
		JsonObject findSession = new JsonObject()
				.putString("action", "find")
				.putString("sessionId", sessionId);
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

	public static void createSession(EventBus eb, String userId, final Handler<String> handler) {
		createSession(eb, userId, null, null, handler);
	}

	public static void createSession(EventBus eb, String userId, String sessionIndex, String nameId,
			final Handler<String> handler) {
		JsonObject json = new JsonObject()
				.putString("action", "create")
				.putString("userId", userId);
		if (sessionIndex != null && nameId != null && !sessionIndex.trim().isEmpty() && !nameId.trim().isEmpty()) {
			json.putString("SessionIndex", sessionIndex).putString("NameID", nameId);
		}
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					if ("ok".equals(res.body().getString("status"))) {
						handler.handle(res.body().getString("sessionId"));
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
				.putString("action", "drop")
				.putString("sessionId", sessionId);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					handler.handle("ok".equals(res.body().getString("status")));
				}
			}
		});
	}

	public static void deleteSessionWithMetadata(EventBus eb, String sessionId,
			final Handler<JsonObject> handler) {
		JsonObject json = new JsonObject()
				.putString("action", "drop")
				.putBoolean("sessionMetadata", true)
				.putString("sessionId", sessionId);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					if ("ok".equals(res.body().getString("status"))) {
						handler.handle(res.body().getObject("sessionMetadata"));
					} else {
						handler.handle(null);
					}
				}
			}
		});
	}

	public static void deletePermanentSession(EventBus eb, String userId, String currentSessionId,
			final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.putString("action", "dropPermanentSessions")
				.putString("userId", userId)
				.putString("currentSessionId", currentSessionId);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					handler.handle("ok".equals(res.body().getString("status")));
				}
			}
		});
	}

	public static void addSessionAttribute(EventBus eb, String userId,
			String key, Object value, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.putString("action", "addAttribute")
				.putString("userId", userId)
				.putString("key", key)
				.putValue("value", value);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					handler.handle("ok".equals(res.body().getString("status")));
				}
			}
		});
	}

	public static void removeSessionAttribute(EventBus eb, String userId,
			String key, final Handler<Boolean> handler) {
		JsonObject json = new JsonObject()
				.putString("action", "removeAttribute")
				.putString("userId", userId)
				.putString("key", key);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if (handler != null) {
					handler.handle("ok".equals(res.body().getString("status")));
				}
			}
		});
	}

}

