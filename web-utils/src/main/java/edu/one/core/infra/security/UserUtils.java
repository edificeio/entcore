package edu.one.core.infra.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.one.core.infra.request.CookieUtils;
import edu.one.core.infra.security.resources.UserInfos;

public class UserUtils {

	// TODO replace this simple request
	public static void findVisibleUsers(EventBus eb, final JsonObject session,
			final Handler<JsonArray> handler) {
		final JsonArray users = new JsonArray();
		if (session != null && session.getString("classId") != null
				&& session.getString("userId") != null) {
			Map<String, Object> params = new HashMap<>();
			params.put("classId", session.getString("classId"));
			String query = "START n=node:node_auto_index(id={classId}) "
					+ "MATCH n<-[:APPARTIENT]-m "
					+ "RETURN distinct m.id as id, m.ENTPersonNom as lastName, "
						+ "m.ENTPersonPrenom as firstName "
					+ "ORDER BY m.ENTPersonNom";
			sendNeo4j(eb, query, params, new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					if (res.body() != null && "ok".equals(res.body().getString("status"))) {
						JsonObject result = res.body().getObject("result");
						for (String key : result.getFieldNames()) {
							JsonObject r = result.getObject(key);
							if (session.getString("userId").equals(r.getString("userId"))) continue;
							r.putString("username",
									r.getString("firstName") + " " + r.getString("lastName"));
							users.add(r);
						}
					}
					handler.handle(users);
				}
			});
		} else {
			handler.handle(users);
		}
	}

	public static void findVisibleUsers(final EventBus eb, HttpServerRequest request,
			final Handler<JsonArray> handler) {
		getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				if (session != null) {
					findVisibleUsers(eb, session, handler);
				} else {
					handler.handle(new JsonArray());
				}
			}
		});
	}

	// TODO replace with real session busmod
	public static void getSession(EventBus eb, final HttpServerRequest request,
			final Handler<JsonObject> handler) {
		if (request instanceof SecureHttpServerRequest &&
				((SecureHttpServerRequest) request).getSession() != null) {
			handler.handle(((SecureHttpServerRequest) request).getSession());
		} else {
			String oneSessionId = CookieUtils.get("oneSessionId", request);
			if (oneSessionId != null) {
				request.pause();
				JsonObject findSession = new JsonObject()
					.putString("action", "find")
					.putString("sessionId", oneSessionId);
				eb.send("wse.mock.session", findSession, new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						JsonObject session = message.body().getObject("session");
						request.resume();
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
			} else {
				handler.handle(null);
			}
		}
	}

	public static UserInfos sessionToUserInfos(JsonObject session) {
		if (session == null) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(session.encode(), UserInfos.class);
		} catch (IOException e) {
			return null;
		}
	}

	private static void sendNeo4j(EventBus eb, String query, Map<String, Object> params,
			Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", new JsonObject(params));
		eb.send("wse.neo4j.persistor", jo, handler);
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

}
