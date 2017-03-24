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

import com.hazelcast.core.BaseMap;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.HazelcastSerializationException;
import fr.wseduc.mongodb.MongoDb;
import org.entcore.common.neo4j.Neo4j;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.core.spi.cluster.ClusterManager;

import java.io.Serializable;
import java.util.*;

public class AuthManager extends BusModBase implements Handler<Message<JsonObject>> {

	protected Map<String, String> sessions;
	protected Map<String, List<LoginInfo>> logins;

	private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;
	private static final String SESSIONS_COLLECTION = "sessions";

	private long sessionTimeout;
	private MongoDb mongo;
	private Neo4j neo4j;

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
		ConcurrentSharedMap<Object, Object> server = vertx.sharedData().getMap("server");
		String neo4jConfig = (String) server.get("neo4jConfig");
		neo4j = Neo4j.getInstance();
		neo4j.init(vertx, new JsonObject(neo4jConfig));
		Boolean cluster = (Boolean) server.get("cluster");
		String node = (String) server.get("node");
		mongo = MongoDb.getInstance();
		mongo.init(vertx.eventBus(), node + config.getString("mongo-address", "wse.mongodb.persistor"));
		if (Boolean.TRUE.equals(cluster)) {
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

		eb.registerLocalHandler(address, this);
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
		case "dropPermanentSessions" :
			doDropPermanentSessions(message);
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

	private void doDropPermanentSessions(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		String currentSessionId = message.body().getString("currentSessionId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doDropPermanentSessions] Invalid userId : " + message.body().encode());
			return;
		}

		JsonObject query = new JsonObject().putString("userId", userId);
		if (currentSessionId != null) {
			query.putObject("_id", new JsonObject().putString("$ne", currentSessionId));
		}
		mongo.delete(SESSIONS_COLLECTION, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					sendOK(message);
				} else {
					sendError(message, event.body().getString("message"));
				}
			}
		});
	}

	private void doFindByUserId(final Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doFindByUserId] Invalid userId : " + message.body().encode());
			return;
		}

		LoginInfo info = getLoginInfo(userId);
		if (info == null && !message.body().getBoolean("allowDisconnectedUser", false)) {
			sendError(message, "[doFindByUserId] info is null - Invalid userId : " + message.body().encode());
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
		JsonObject session = null;
		try {
			session = unmarshal(sessions.get(info.sessionId));
		} catch (HazelcastSerializationException e) {
			logger.error("Error in deserializing hazelcast session " + info.sessionId, e);
		}
		if (session == null) {
			sendError(message, "Session not found. 6");
			return;
		}
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

	private LoginInfo getLoginInfo(String userId) {
		List<LoginInfo> loginInfos = logins.get(userId);
		if (loginInfos != null && !loginInfos.isEmpty()) {
			return loginInfos.get(loginInfos.size() - 1);
		}
		return null;
	}

	private JsonObject unmarshal(String s) {
		if (s != null) {
			return new JsonObject(s);
		}
		return null;
	}

	private void doFind(final Message<JsonObject> message) {
		final String sessionId = message.body().getString("sessionId");
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session = null;
		try {
			session = unmarshal(sessions.get(sessionId));
		} catch (HazelcastSerializationException e) {
			logger.warn("Error in deserializing hazelcast session " + sessionId);
			try {
				if (sessions instanceof BaseMap) {
					((BaseMap) sessions).delete(sessionId);
				} else {
					sessions.remove(sessionId);
				}
			} catch (HazelcastSerializationException e1) {
				logger.warn("Error getting object after removing hazelcast session " + sessionId);
			}
		}

		if (session == null) {
			final JsonObject query = new JsonObject().putString("_id", sessionId);
			mongo.findOne(SESSIONS_COLLECTION, query, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject res = event.body().getObject("result");
					String userId;
					if ("ok".equals(event.body().getString("status")) && res != null &&
							(userId = res.getString("userId")) != null && !userId.trim().isEmpty()) {
						final String uId = userId;
						createSession(userId, sessionId, res.getString("SessionIndex"), res.getString("NameID"),
								new Handler<String>() {
							@Override
							public void handle(String sId) {
								if (sId != null) {
									try {
										JsonObject s = unmarshal(sessions.get(sId));
										if (s != null) {
											JsonObject sessionResponse = new JsonObject().putString("status", "ok")
													.putObject("session", s);
											sendOK(message, sessionResponse);
										} else {
											sendError(message, "Session not found. 1");
										}
									} catch (HazelcastSerializationException e) {
										logger.warn("Error in deserializing new hazelcast session " + sId);
										generateSessionInfos(uId, new Handler<JsonObject>() {

											@Override
											public void handle(JsonObject event) {
												if (event != null) {
													logger.info("Session with hazelcast problem : " + event.encode());
													sendOK(message, new JsonObject().putString("status", "ok")
															.putObject("session", event));
												} else {
													sendError(message, "Session not found. 2");
												}
											}
										});
									}
								} else {
									sendError(message, "Session not found. 3");
								}
							}
						});
					} else {
						sendError(message, "Session not found. 4");
					}
				}
			});
		} else {
			sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
		}
	}

	private void doCreate(final Message<JsonObject> message) {
		final String userId = message.body().getString("userId");
		final String sessionIndex = message.body().getString("SessionIndex");
		final String nameID = message.body().getString("NameID");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[doCreate] Invalid userId : " + message.body().encode());
			return;
		}

		createSession(userId, null, sessionIndex, nameID, new Handler<String>() {
			@Override
			public void handle(String sessionId) {
				if (sessionId != null) {
					sendOK(message, new JsonObject()
							.putString("status", "ok")
							.putString("sessionId", sessionId));
				} else {
					sendError(message, "Invalid userId : " + userId);
				}
			}
		});
	}

	private void createSession(final String userId, final String sId, final String sessionIndex, final String nameId, final Handler<String> handler) {
		final String sessionId = (sId != null) ? sId : UUID.randomUUID().toString();
		generateSessionInfos(userId, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject infos) {
				if (infos != null) {
					long timerId = vertx.setTimer(sessionTimeout, new Handler<Long>() {
						public void handle(Long timerId) {
							logins.remove(userId);
							sessions.remove(sessionId);
						}
					});
					try {
						sessions.put(sessionId, infos.encode());
						addLoginInfo(userId, timerId, sessionId);
					} catch (HazelcastSerializationException e) {
						logger.error("Error putting session in hazelcast map");
						try {
							if (sessions instanceof IMap) {
								((IMap) sessions).putAsync(sessionId, infos.encode());
							}
							addLoginInfo(userId, timerId, sessionId);
						} catch (HazelcastSerializationException e1) {
							logger.error("Error putting async session in hazelcast map", e1);
						}
					}
					final JsonObject now = MongoDb.now();
					if (sId == null) {
						JsonObject json = new JsonObject()
								.putString("_id", sessionId).putString("userId", userId)
								.putObject("created", now).putObject("lastUsed", now);
						if (sessionIndex != null && nameId != null) {
							json.putString("SessionIndex", sessionIndex).putString("NameID", nameId);
						}
						mongo.save(SESSIONS_COLLECTION, json);
					} else {
						mongo.update(SESSIONS_COLLECTION, new JsonObject().putString("_id", sessionId),
								new JsonObject().putObject("$set", new JsonObject().putObject("lastUsed", now)));
					}
					handler.handle(sessionId);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	private void addLoginInfo(String userId, long timerId, String sessionId) {
		List<LoginInfo> loginInfos = logins.get(userId);
		if (loginInfos == null) {
			loginInfos = new ArrayList<>();
		}
		loginInfos.add(new LoginInfo(timerId, sessionId));
		logins.put(userId, loginInfos);
	}

	private void doDrop(final Message<JsonObject> message) {
		final String sessionId = message.body().getString("sessionId");
		boolean sessionMeta =  message.body().getBoolean("sessionMetadata", false);
		if (sessionId == null || sessionId.trim().isEmpty()) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		if (sessionMeta) {
			final JsonObject query = new JsonObject().putString("_id", sessionId);
			mongo.findOne(SESSIONS_COLLECTION, query, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject res = event.body().getObject("result");
					dropSession(message, sessionId, res);
				}
			});
		} else {
			dropSession(message, sessionId, null);
		}
	}

	private void dropSession(Message<JsonObject> message, String sessionId, JsonObject meta) {
		mongo.delete(SESSIONS_COLLECTION, new JsonObject().putString("_id", sessionId));
		JsonObject session =  null;
		try {
			session = unmarshal(sessions.get(sessionId));
		} catch (HazelcastSerializationException e) {
			try {
				if (sessions instanceof BaseMap) {
					((BaseMap) sessions).delete(sessionId);
				} else {
					sessions.remove(sessionId);
				}
			} catch (HazelcastSerializationException e1) {
				logger.error("In doDrop - Error getting object after removing hazelcast session " + sessionId, e);
			}
		}
		if (session != null) {
			JsonObject s = unmarshal(sessions.remove(sessionId));
			if (s != null) {
				final String userId = s.getString("userId");
				LoginInfo info = removeLoginInfo(sessionId, userId);
				if (config.getBoolean("slo", false)) {
					eb.send("cas", new JsonObject().putString("action", "logout").putString("userId", userId));
				}
				if (info != null) {
					vertx.cancelTimer(info.timerId);
				}
			}
		}
		JsonObject res = new JsonObject().putString("status", "ok");
		if (meta != null) {
			res.putObject("sessionMetadata", meta);
		}
		sendOK(message, res);
	}

	private LoginInfo removeLoginInfo(String sessionId, String userId) {
		List<LoginInfo> loginInfos = logins.get(userId);
		LoginInfo loginInfo = null;
		if (loginInfos != null && sessionId != null) {
			boolean found = false;
			int idx = 0;
			for (LoginInfo i : loginInfos) {
				if (sessionId.equals(i.sessionId)) {
					found = true;
					break;
				}
				idx++;
			}
			if (found) {
				loginInfo = loginInfos.remove(idx);
				if (loginInfos.isEmpty()) {
					logins.remove(userId);
				} else {
					logins.put(userId, loginInfos);
				}
			}
		}
		return loginInfo;
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
			sendError(message, "[getSessionByUserId] Invalid userId : " + message.body().encode());
			return null;
		}

		LoginInfo info = getLoginInfo(userId);
		if (info == null) { // disconnected user : ignore action
			sendOK(message);
			return null;
		}
		JsonObject session =  null;
		try {
			session = unmarshal(sessions.get(info.sessionId));
		} catch (HazelcastSerializationException e) {
			logger.error("Error in deserializing hazelcast session " + info.sessionId, e);
		}
		if (session == null) {
			sendError(message, "Session not found. 7");
			return null;
		}
		return session;
	}

	private void updateSessionByUserId(Message<JsonObject> message, JsonObject session) {
		final String userId = message.body().getString("userId");
		if (userId == null || userId.trim().isEmpty()) {
			sendError(message, "[updateSessionByUserId] Invalid userId : " + message.body().encode());
			return;
		}

		List<LoginInfo> infos = logins.get(userId);
		if (infos == null || infos.isEmpty()) {
			sendError(message, "[updateSessionByUserId] info is null - Invalid userId : " + message.body().encode());
			return;
		}
		for (LoginInfo info : infos) {
			try {
				sessions.put(info.sessionId, session.encode());
			} catch (HazelcastSerializationException e) {
				logger.error("Error putting session in hazelcast map : " + info.sessionId, e);
			}
		}
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
		final String query =
				"MATCH (n:User {id : {id}}) " +
				"WHERE HAS(n.login) " +
				"OPTIONAL MATCH n-[:IN]->(gp:Group) " +
				"OPTIONAL MATCH gp-[:DEPENDS]->(s:Structure) " +
				"OPTIONAL MATCH gp-[:DEPENDS]->(c:Class) " +
				"OPTIONAL MATCH n-[rf:HAS_FUNCTION]->fg-[:CONTAINS_FUNCTION*0..1]->(f:Function) " +
				"OPTIONAL MATCH n<-[:RELATED]-(child:User) " +
				"RETURN distinct " +
				"n.classes as classNames, n.level as level, n.login as login, COLLECT(distinct c.id) as classes, " +
				"n.lastName as lastName, n.firstName as firstName, n.externalId as externalId, n.federated as federated, " +
				"n.birthDate as birthDate, " +
				"n.displayName as username, HEAD(n.profiles) as type, COLLECT(distinct [child.id, child.lastName, child.firstName]) as childrenInfo, " +
				"COLLECT(distinct s.id) as structures, COLLECT(distinct [f.externalId, rf.scope]) as functions, " +
				"COLLECT(distinct s.name) as structureNames, COLLECT(distinct s.UAI) as uai, " +
				"COLLECT(distinct gp.id) as groupsIds, n.federatedIDP as federatedIDP, n.functions as aafFunctions";
		final String query2 =
				"MATCH (n:User {id : {id}})-[:IN]->()-[:AUTHORIZED]->(:Role)-[:AUTHORIZE]->(a:Action)" +
				"<-[:PROVIDE]-(app:Application) " +
				"WHERE HAS(n.login) " +
				"RETURN DISTINCT COLLECT(distinct [a.name,a.displayName,a.type]) as authorizedActions, " +
				"COLLECT(distinct [app.name,app.address,app.icon,app.target,app.displayName,app.display,app.prefix]) as apps";
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
		params.putString("id", userId);
		JsonArray statements = new JsonArray()
				.add(new JsonObject().putString("statement", query).putObject("parameters", params))
				.add(new JsonObject().putString("statement", query2).putObject("parameters", params))
				.add(new JsonObject().putString("statement", query3).putObject("parameters", params))
				.add(new JsonObject().putString("statement", query4))
				.add(new JsonObject().putString("statement", query5).putObject("parameters", params));
		neo4j.executeTransaction(statements, null, true, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray results = message.body().getArray("results");
				if ("ok".equals(message.body().getString("status")) && results != null && results.size() == 5 &&
						results.<JsonArray>get(0).size() > 0 && results.<JsonArray>get(1).size() > 0) {
					JsonObject j = results.<JsonArray>get(0).get(0);
					JsonObject j2 = results.<JsonArray>get(1).get(0);
					JsonObject j3 = results.<JsonArray>get(2).get(0);
					JsonObject structureMapping = new JsonObject();
					for (Object o : results.<JsonArray>get(3)) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject jsonObject = (JsonObject) o;
						structureMapping.putString(jsonObject.getString("externalId"), jsonObject.getString("id"));
					}
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
								.putBoolean("display", ((a.get(5) == null) || (boolean) a.get(5)))
								.putString("prefix", (String) a.get(6))
						);
					}
					for (Object o : j.getArray("aafFunctions", new JsonArray())) {
						if (o == null) continue;
						String [] sf = o.toString().split("\\$");
						if (sf.length == 5) {
							JsonObject jo = functions.getObject(sf[1]);
							if (jo == null) {
								jo = new JsonObject().putString("code", sf[1])
										.putString("functionName", sf[2])
										.putArray("scope", new JsonArray())
										.putArray("structureExternalIds", new JsonArray())
										.putObject("subjects", new JsonObject());
								functions.putObject(sf[1], jo);
							}
							JsonObject subject = jo.getObject("subjects").getObject(sf[3]);
							if (subject == null) {
								subject = new JsonObject()
										.putString("subjectCode", sf[3])
										.putString("subjectName", sf[4])
										.putArray("scope", new JsonArray())
										.putArray("structureExternalIds", new JsonArray());
								jo.getObject("subjects").putObject(sf[3], subject);
							}
							jo.getArray("structureExternalIds").addString(sf[0]);
							subject.getArray("structureExternalIds").addString(sf[0]);
							String sid = structureMapping.getString(sf[0]);
							if (sid != null) {
								jo.getArray("scope").addString(sid);
								subject.getArray("scope").addString(sid);
							}
						}
					}
					j.removeField("aafFunctions");
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
					final JsonObject children = new JsonObject();
					final List<String> childrenIds = new ArrayList<String>();
					for (Object o : j.getArray("childrenInfo", new JsonArray())) {
						if (!(o instanceof JsonArray)) continue;
						final JsonArray a = (JsonArray) o;
						final String childId = a.get(0);
						if (childId != null) {
							childrenIds.add(childId);
							JsonObject jo = children.getObject(childId);
							if (jo == null) {
								jo = new JsonObject()
										.putString("lastName", (String) a.get(1))
										.putString("firstName", (String) a.get(2));
								children.putObject(childId, jo);
							}
						}
					}
					j.removeField("childrenInfo");

					j.putObject("functions", functions);
					j.putArray("authorizedActions", actions);
					j.putArray("apps", apps);
					j.putArray("childrenIds", new JsonArray(childrenIds));
					j.putObject("children", children);
					final JsonObject cache = (results.<JsonArray>get(4) != null && results.<JsonArray>get(4).size() > 0 &&
							results.<JsonArray>get(4).get(0) != null) ? results.<JsonArray>get(4).<JsonObject>get(0) : new JsonObject();
					j.putObject("cache", cache);
					j.putArray("widgets", j3.getArray("widgets", new JsonArray()));
					handler.handle(j);
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
