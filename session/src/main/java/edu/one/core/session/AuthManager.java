package edu.one.core.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

	private String address;
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

		this.address = getOptionalStringConfig("address", "wse.session");
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
				"START n=node:node_auto_index(id={id}) " +
				"MATCH n-[?:APPARTIENT]->g-[:AUTHORIZED]->r-[:AUTHORIZE]->a<-[:PROVIDE]-app " +
				"WITH app, a, n " +
				"MATCH n-[?:APPARTIENT]->gp " +
				"WITH app, a, n, gp " +
				"MATCH n-[?:APPARTIENT]->gpe-[:DEPENDS]->s " +
				"WHERE (s IS NULL OR (has(s.type) AND s.type = 'ETABEDUCNAT')) AND " +
				"(gp IS NULL OR (has(gp.type) AND gp.type IN ['GROUP_CLASSE_PERSRELELEVE','GROUP_CLASSE_ELEVE'," +
						"'GROUP_CLASSE_PERSEDUCNAT','GROUP_CLASSE_ENSEIGNANT','GROUP_ETABEDUCNAT_PERSRELELEVE'," +
						"'GROUP_ETABEDUCNAT_ELEVE','GROUP_ETABEDUCNAT_PERSEDUCNAT','GROUP_ETABEDUCNAT_ENSEIGNANT'," +
						"'GROUP_ETABEDUCNAT_DIRECTEUR'])) " +
				"RETURN distinct a.name as name, a.displayName as displayName, " +
				"a.type as type, n.ENTPersonClasses? as classe, " +
				"n.ENTPersonNom as lastname, n.ENTPersonPrenom as firstname, " +
				"n.ENTPersonNomAffichage as username, n.type as userType, n.ENTEleveNiveau? as level, " +
				"n.ENTPersonLogin as login, app.name? as appName, app.address? as appAddress, " +
				"app.icon? as appIcon, app.target? as appTarget, " +
				"s.ENTStructureNomCourant? as schoolName, s.UAI? as uai, COLLECT(distinct gp.id?) as profilGroupsIds";
		Map<String, Object> params = new HashMap<>();
		params.put("id", userId);
		sendNeo4j(query, params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonObject result = message.body().getObject("result");
				if ("ok".equals(message.body().getString("status")) && result != null &&
						!result.getFieldNames().isEmpty()) {
					JsonObject j = message.body().getObject("result").getObject("0");
					JsonArray apps = new JsonArray();
					Set<String> appsNames = new HashSet<>();
					JsonObject infos = new JsonObject()
						.putString("userId", userId)
						.putString("firstName", j.getString("firstname"))
						.putString("lastName", j.getString("lastname"))
						.putString("username", j.getString("username"))
						.putString("classId", j.getString("classe"))
						.putString("login", j.getString("login"))
						.putString("level", j.getString("level"))
						.putString("schoolName", j.getString("schoolName"))
						.putString("uai", j.getString("uai"))
						.putArray("profilGroupsIds", j.getArray("profilGroupsIds"))
						.putString("type", j.getString("userType"));
					JsonArray actions = new JsonArray();
					for (String attr : result.getFieldNames()) {
						JsonObject json = result.getObject(attr);
						json.removeField("firstname");
						json.removeField("lastname");
						json.removeField("username");
						json.removeField("classe");
						json.removeField("login");
						json.removeField("level");
						json.removeField("schoolName");
						json.removeField("uai");
						json.removeField("profilGroupsIds");
						json.removeField("userType");
						String appName = json.getString("appName");
						String appAddress = json.getString("appAddress");
						String appIcon = json.getString("appIcon");
						String appTarget = json.getString("appTarget");
						json.removeField("appName");
						json.removeField("appAddress");
						json.removeField("appIcon");
						json.removeField("appTarget");
						if (!appsNames.contains(appName)) {
							appsNames.add(appName);
							apps.addObject(new JsonObject()
							.putString("name", appName)
							.putString("address", appAddress)
							.putString("icon", appIcon)
							.putString("target", appTarget));
						}
						actions.add(json);
					}
					infos.putArray("apps", apps);
					handler.handle(infos.putArray("authorizedActions", actions));
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
