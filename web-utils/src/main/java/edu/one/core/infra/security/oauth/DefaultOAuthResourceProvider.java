package edu.one.core.infra.security.oauth;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.security.SecureHttpServerRequest;

public class DefaultOAuthResourceProvider implements OAuthResourceProvider {

	private final EventBus eb;
	private static final String OAUTH_ADDRESS = "wse.oauth";

	public DefaultOAuthResourceProvider(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void validToken(final SecureHttpServerRequest request, final Handler<Boolean> handler) {
		JsonObject headers = new JsonObject();
		for (String name : request.headers().names()) {
			headers.putString(name, request.headers().get(name));
		}
		JsonObject params = new JsonObject();
		for (String name : request.params().names()) {
			params.putString(name, request.params().get(name));
		}
		JsonObject json = new JsonObject()
		.putObject("headers", headers)
		.putObject("params", params);
		request.pause();
		eb.send(OAUTH_ADDRESS, json, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				request.resume();
				if ("ok".equals(res.body().getString("status"))) {
					request.setAttribute("client_id", res.body().getString("client_id"));
					request.setAttribute("remote_user", res.body().getString("remote_user"));
					request.setAttribute("scope", res.body().getString("scope"));
					handler.handle(true);
				} else {
					handler.handle(false);
				}
			}
		});
	}

}
