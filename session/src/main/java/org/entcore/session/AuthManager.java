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

package org.entcore.session;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.spi.cluster.ClusterManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthManager extends BusModBase implements Handler<Message<JsonObject>> {

	protected Map<String, String> sessions;
	protected Map<String, LoginInfo> logins;

	private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

	private long sessionTimeout;

	private static final class LoginInfo implements Serializable {
		final long timerId;
		final String sessionId;

		private LoginInfo(long timerId, String sessionId) {
			this.timerId = timerId;
			this.sessionId = sessionId;
		}
	}

	public void start() {
		super.start();

		if (getOptionalBooleanConfig("cluster", false)) {
			ClusterManager cm = ((VertxInternal) vertx).clusterManager();
			sessions = cm.getSyncMap("sessions");
			logins = cm.getSyncMap("logins");
		} else {
			sessions = new HashMap<>();
			logins = new HashMap<>();
		}
		final String address = getOptionalStringConfig("address", "wse.session");
		Number timeout = config.getNumber("session_timeout");
		if (timeout != null) {
			if (timeout instanceof Long) {
				this.sessionTimeout = (Long)timeout;
			} else if (timeout instanceof Integer) {
				this.sessionTimeout = (Integer)timeout;
			}
		} else {
			this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
		}

		eb.registerHandler(address, this);
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
			doFindByUserId(message);
			break;
		case "create":
			doCreate(message);
			break;
		case "drop":
			doDrop(message);
			break;
		case "addAttribute":
			doAddAttribute(message);
			break;
		case "removeAttribute":
			doRemoveAttribute(message);
			break;
		default:
			sendError(message, "Invalid action: " + action);
		}
	}

	private void doFindByUserId(final Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "Invalid userId.");
			return;
		}

		LoginInfo info = logins.get(userId);
		if (info == null && !message.body().getBoolean("allowDisconnectedUser", false)) {
			sendError(message, "Invalid userId.");
			return;
		} else if (info == null) {
			generateSessionInfos(userId, new Handler<JsonObject>() {

				@Override
				public void handle(JsonObject infos) {
					if (infos != null) {
						sendOK(message, new JsonObject().putString("status", "ok")
								.putObject("session", infos));
					} else {
						sendError(message, "Invalid userId : " + userId);
					}
				}
			});
			return;
		}
		JsonObject session = unmarshal(sessions.get(info.sessionId));
		if (session == null) {
			sendError(message, "Session not found.");
			return;
		}
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

	private JsonObject unmarshal(String s) {
		if (s != null) {
			return new JsonObject(s);
		}
		return null;
	}

	private void doFind(Message<JsonObject> message) {
		String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session =  unmarshal(sessions.get(sessionId));
		if (session == null) {
			sendError(message, "Session not found.");
			return;
		}
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

	private void doCreate(final Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "Invalid userId.");
			return;
		}

		generateSessionInfos(userId, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject infos) {
				if (infos != null) {
					final String sessionId = UUID.randomUUID().toString();
					long timerId = vertx.setTimer(sessionTimeout, new Handler<Long>() {
						public void handle(Long timerId) {
							sessions.remove(sessionId);
							logins.remove(userId);
						}
					});
					sessions.put(sessionId, infos.encode());
					logins.put(userId, new LoginInfo(timerId, sessionId));
					sendOK(message, new JsonObject()
					.putString("status", "ok")
					.putString("sessionId", sessionId));
				} else {
					sendError(message, "Invalid userId : " + userId);
				}
			}
		});
	}

	private void doDrop(Message<JsonObject> message) {
		String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session =  unmarshal(sessions.get(sessionId));
		if (session == null) {
			sendError(message, "Session not found.");
			return;
		}

		JsonObject s =  unmarshal(sessions.remove(sessionId));
		if (s != null) {
			final String userId = s.getString("userId");
			LoginInfo info = logins.remove(userId);
			if (config.getBoolean("slo", false)) {
				eb.send("cas", new JsonObject().putString("action", "logout").putString("userId", userId));
			}
			if (info != null) {
				vertx.cancelTimer(info.timerId);
			}
		}
		sendOK(message, new JsonObject().putString("status", "ok"));
	}


	private void doAddAttribute(Message<JsonObject> message) {
		JsonObject session = getSessionByUserId(message);
		if (session == null) {
			return;
		}

		String key = message.body().getString("key");
		if (key == null || key.trim().isEmpty()) {
			sendError(message, "Invalid key.");
			return;
		}

		Object value = message.body().getValue("value");
		if (value == null) {
			sendError(message, "Invalid value.");
			return;
		}

		session.getObject("cache").putValue(key, value);

		updateSessionByUserId(message, session);
		sendOK(message);
	}

	private JsonObject getSessionByUserId(Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "Invalid userId.");
			return null;
		}

		LoginInfo info = logins.get(userId);
		if (info == null) {
			sendError(message, "Invalid userId.");
			return null;
		}
		JsonObject session =  unmarshal(sessions.get(info.sessionId));
		if (session == null) {
			sendError(message, "Session not found.");
			return null;
		}
		return session;
	}

	private void updateSessionByUserId(Message<JsonObject> message, JsonObject session) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "Invalid userId.");
			return;
		}

		LoginInfo info = logins.get(userId);
		if (info == null) {
			sendError(message, "Invalid userId.");
			return;
		}
		sessions.put(info.sessionId, session.encode());
	}

	private void doRemoveAttribute(Message<JsonObject> message) {
		JsonObject session = getSessionByUserId(message);
		if (session == null) {
			return;
		}

		String key = message.body().getString("key");
		if (key == null || key.trim().isEmpty()) {
			sendError(message, "Invalid key.");
			return;
		}

		session.getObject("cache").removeField(key);
		updateSessionByUserId(message, session);
		sendOK(message);
	}

	private void generateSessionInfos(final String userId, final Handler<JsonObject> handler) {
		String query =
				"MATCH (n:User {id : {id}}) " +
				"WHERE HAS(n.login) " +
				"OPTIONAL MATCH n-[:IN]->(gp:Group) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH n-[:IN]->()-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) " +
				"OPTIONAL MATCH n-[:IN]->()-[:HAS_PROFILE]->(p:Profile) " +
				"RETURN distinct " +
				"n.classes as classNames, n.level as level, n.login as login, COLLECT(distinct c.id) as classes, " +
				"n.lastName as lastName, n.firstName as firstName, n.externalId as externalId, " +
				"n.displayName as username, HEAD(COLLECT(distinct p.name)) as type, " +
				"COLLECT(distinct s.id) as structures, COLLECT(distinct [f.externalId, rf.scope]) as functions, " +
				"COLLECT(distinct s.name) as structureNames, COLLECT(distinct s.UAI) as uai, " +
				"COLLECT(distinct gp.id) as groupsIds";
		String query2 =
				"MATCH (n:User {id : {id}})-[:IN]->()-[:AUTHORIZED]->()-[:AUTHORIZE]->a<-[:PROVIDE]-app " +
				"WHERE HAS(n.login) " +
				"RETURN DISTINCT COLLECT(distinct [a.name,a.displayName,a.type]) as authorizedActions, " +
				"COLLECT(distinct [app.name,app.address,app.icon,app.target,app.displayName]) as apps";
		JsonObject params = new JsonObject();
		params.putString("id", userId);
		JsonArray statements = new JsonArray()
				.add(new JsonObject().putString("statement", query).putObject("parameters", params))
				.add(new JsonObject().putString("statement", query2).putObject("parameters", params));
		executeTransaction(statements, null, true, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray results = message.body().getArray("results");
				if ("ok".equals(message.body().getString("status")) && results != null && results.size() == 2 &&
						results.<JsonArray>get(0).size() > 0 && results.<JsonArray>get(1).size() > 0) {
					JsonObject j = results.<JsonArray>get(0).get(0);
					JsonObject j2 = results.<JsonArray>get(1).get(0);
					j.putString("userId", userId);
					JsonObject functions = new JsonObject();
					JsonArray actions = new JsonArray();
					JsonArray apps = new JsonArray();
					for (Object o : j2.getArray("authorizedActions", new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						actions.addObject(new JsonObject()
								.putString("name", (String) a.get(0))
								.putString("displayName", (String) a.get(1))
								.putString("type", (String) a.get(2)));
					}
					for (Object o : j2.getArray("apps", new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						apps.addObject(new JsonObject()
								.putString("name", (String) a.get(0))
								.putString("address", (String) a.get(1))
								.putString("icon", (String) a.get(2))
								.putString("target", (String) a.get(3))
								.putString("displayName", (String) a.get(4))
						);
					}
					for (Object o : j.getArray("functions", new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						String code = a.get(0);
						if (code != null) {
							functions.putObject(code, new JsonObject()
									.putString("code", code)
									.putArray("scope", (JsonArray) a.get(1))
							);
						}
					}
					j.putObject("functions", functions);
					j.putArray("authorizedActions", actions);
					j.putArray("apps", apps);
					j.putObject("cache", new JsonObject());
					handler.handle(j);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
			Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeTransaction");
		jo.putArray("statements", statements);
		jo.putBoolean("commit", commit);
		if (transactionId != null) {
			jo.putNumber("transactionId", transactionId);
		}
		eb.send("wse.neo4j.persistor", jo, handler);
	}

}
