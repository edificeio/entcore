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

	// TODO replace this mock
	public static void findVisibleUsers(JsonObject session, Handler<JsonArray> handler) {
		JsonArray users = new JsonArray();
		if (session != null &&
				"42d93f59-9b12-417d-b998-45b18bdd5afa".equals(session.getString("userId"))) {
			for (int i = 0; i < 3; i++) {
				JsonObject user = new JsonObject()
					.putString("id", "42d93f59-9b12-417d-b998-45b18bdd5af" + i)
					.putString("username", "blip" + i);
				users.add(user);
			}
		}
		handler.handle(users);
	}

	public static void findVisibleUsers(EventBus eb, HttpServerRequest request,
			final Handler<JsonArray> handler) {
		getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				if (session != null) {
					findVisibleUsers(session, handler);
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
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(session.encode(), UserInfos.class);
		} catch (IOException e) {
			return null;
		}
	}

}
