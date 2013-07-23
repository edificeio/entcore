package edu.one.core.infra.request.filter;

import java.util.Set;
import java.util.regex.Matcher;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.http.Binding;
import edu.one.core.infra.request.CookieUtils;

public class ActionFilter implements Filter {

	private final Set<Binding> bindings;
	private final EventBus eb;

	public ActionFilter(Set<Binding> bindings, EventBus eb) {
		this.bindings = bindings;
		this.eb = eb;
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		String oneSessionId = CookieUtils.get("oneSessionId", request);
		if (oneSessionId != null) {
			JsonObject findSession = new JsonObject();
			findSession.putString("action", "find").
				putString("sessionId", oneSessionId);
			eb.send("wse.mock.session", findSession, new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> message) {
					JsonObject session = message.body().getObject("session");
					if ("ok".equals(message.body().getString("status")) && session != null) {
						handler.handle(userIsAuthorized(request, session));
					} else {
						handler.handle(false);
					}
				}
			});
		} else {
			handler.handle(false);
		}
	}

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(401).end();
	}

	private Boolean userIsAuthorized(HttpServerRequest request, JsonObject session) {
		Binding binding = requestBinding(request);
		JsonArray actions = session.getArray("authorizedActions");
		if (binding != null && binding.getServiceMethod() != null
				&& actions != null && actions.size() > 0) {
			for (Object a: actions) {
				JsonObject action = (JsonObject) a;
				if (binding.getServiceMethod().equals(action.getString("name"))) {
					return true;
				}
			}
		}
		return false;
	}

	private Binding requestBinding(HttpServerRequest request) {
		for (Binding binding: bindings) {
			if (!request.method().equals(binding.getMethod().name())) {
				continue;
			}
			Matcher m = binding.getUriPattern().matcher(request.path());
			if (m.matches()) {
				return binding;
			}
		}
		return null;
	}

}
