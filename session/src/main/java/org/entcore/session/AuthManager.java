package org.entcore.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class AuthManager extends BusModBase implements Handler<Message<JsonObject>> {

	protected final Map<String, JsonObject> sessions = new HashMap<>();
	protected final Map<String, LoginInfo> logins = new HashMap<>();

	private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

	private long sessionTimeout;

	private static final class LoginInfo {
		final long timerId;
		final String sessionId;

		private LoginInfo(long timerId, String sessionId) {
			this.timerId = timerId;
			this.sessionId = sessionId;
		}
	}

	public void start() {
		super.start();

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
		JsonObject session = sessions.get(info.sessionId);
		if (session == null) {
			sendError(message, "Session not found.");
			return;
		}
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

	private void doFind(Message<JsonObject> message) {
		String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session = sessions.get(sessionId);
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
					sessions.put(sessionId, infos);
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

		JsonObject session = sessions.get(sessionId);
		if (session == null) {
			sendError(message, "Session not found.");
			return;
		}

		JsonObject s = sessions.remove(sessionId);
		if (s != null) {
			LoginInfo info = logins.remove(s.getString("userId"));
			if (info != null) {
				vertx.cancelTimer(info.timerId);
			}
		}
		sendOK(message, new JsonObject().putString("status", "ok"));
	}

	private void generateSessionInfos(final String userId, final Handler<JsonObject> handler) {
		String query =
				"MATCH (n:User) " +
				"WHERE n.id = {id} AND HAS(n.login) " +
				"OPTIONAL MATCH n-[:APPARTIENT]->g-[:AUTHORIZED]->r-[:AUTHORIZE]->a<-[:PROVIDE]-app " +
				"WITH app, a, n " +
				"OPTIONAL MATCH n-[:APPARTIENT]->(gp:ProfileGroup) " +
				"WITH app, a, n, gp " +
				"OPTIONAL MATCH n-[:APPARTIENT]->gpe-[:DEPENDS]->(s:School) " +
				"RETURN distinct COLLECT(distinct [a.name,a.displayName,a.type]) as authorizedActions, " +
				"HEAD(n.classes) as classId, n.level as level, n.login as login, " +
				"n.lastName as lastName, n.firstName as firstName, " +
				"n.displayName as username, HEAD(filter(x IN labels(n) WHERE x <> 'Visible' AND x <> 'User')) as type, " +
				"COLLECT(distinct [app.name,app.address,app.icon,app.target,app.displayName]) as apps, " +
				"s.name as schoolName, s.UAI as uai, COLLECT(distinct gp.id) as profilGroupsIds";
		Map<String, Object> params = new HashMap<>();
		params.put("id", userId);
		sendNeo4j(query, params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray result = message.body().getArray("result");
				if ("ok".equals(message.body().getString("status")) && result != null && result.size() > 0) {
					JsonObject j = result.get(0);
					j.putString("userId", userId);
					JsonArray actions = new JsonArray();
					JsonArray apps = new JsonArray();
					for (Object o : j.getArray("authorizedActions", new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						JsonArray a = (JsonArray) o;
						actions.addObject(new JsonObject()
								.putString("name", (String) a.get(0))
								.putString("displayName", (String) a.get(1))
								.putString("type", (String) a.get(2)));
					}
					for (Object o : j.getArray("apps", new JsonArray())) {
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
					j.putArray("authorizedActions", actions);
					j.putArray("apps", apps);
					handler.handle(j);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	private void sendNeo4j(String query, Map<String, Object> params,
			Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", new JsonObject(params));
		eb.send("wse.neo4j.persistor", jo, handler);
	}

}
