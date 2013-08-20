package edu.one.core.infra.security;

import java.io.IOException;

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

	private static final JsonArray usersTypes = new JsonArray()
			.addString("PERSRELELEVE")
			.addString("ELEVE")
			.addString("PERSEDUCNAT");
	private static final String SESSION_ADDRESS = "wse.session";

	public static void findVisibleUsers(EventBus eb, final JsonObject session,
			final Handler<JsonArray> handler) {
		if (session != null && session.getString("userId") != null
				&& !session.getString("userId").trim().isEmpty()) {
			JsonObject m = new JsonObject()
			.putString("userId", session.getString("userId"))
			.putArray("expectedTypes", usersTypes);
			eb.send("wse.communication.users", m, new Handler<Message<JsonArray>>() {

				@Override
				public void handle(Message<JsonArray> res) {
					handler.handle(res.body());
				}
			});
		} else {
			handler.handle(new JsonArray());
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

	public static void getSession(EventBus eb, final HttpServerRequest request,
			final Handler<JsonObject> handler) {
		if (request instanceof SecureHttpServerRequest &&
				((SecureHttpServerRequest) request).getSession() != null) {
			handler.handle(((SecureHttpServerRequest) request).getSession());
		} else {
			String oneSessionId = CookieUtils.get("oneSessionId", request);
			String remoteUserId = null;
			if (request instanceof SecureHttpServerRequest) {
				remoteUserId = ((SecureHttpServerRequest) request).getAttribute("remote_user");
			}
			if ((oneSessionId == null || oneSessionId.trim().isEmpty()) &&
					(remoteUserId == null || remoteUserId.trim().isEmpty())) {
				handler.handle(null);
				return;
			} else {
				request.pause();
				JsonObject findSession = new JsonObject();
				if (oneSessionId != null && !oneSessionId.trim().isEmpty()) {
					findSession.putString("action", "find")
						.putString("sessionId", oneSessionId);
				} else { // remote user (oauth)
					findSession.putString("action", "findByUserId")
					.putString("userId", remoteUserId);
				}
				eb.send(SESSION_ADDRESS, findSession, new Handler<Message<JsonObject>>() {

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

	public static void createSession(EventBus eb, String userId,
			final Handler<String> handler) {
		JsonObject json = new JsonObject()
		.putString("action", "create")
		.putString("userId", userId);
		eb.send(SESSION_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					handler.handle(res.body().getString("sessionId"));
				} else {
					handler.handle(null);
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
				handler.handle("ok".equals(res.body().getString("status")));
			}
		});
	}

}
