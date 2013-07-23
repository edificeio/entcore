package edu.one.core.infra.request.filter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.one.core.infra.http.Binding;
import edu.one.core.infra.request.CookieUtils;
import edu.one.core.infra.security.ActionType;
import edu.one.core.infra.security.resources.ResourcesProvider;
import edu.one.core.infra.security.resources.UserInfos;

public class ActionFilter implements Filter {

	private final Set<Binding> bindings;
	private final EventBus eb;
	private final ResourcesProvider provider;

	public ActionFilter(Set<Binding> bindings, EventBus eb, ResourcesProvider provider) {
		this.bindings = bindings;
		this.eb = eb;
		this.provider = provider;
	}

	public ActionFilter(Set<Binding> bindings, EventBus eb) {
		this(bindings, eb, null);
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
						userIsAuthorized(request, session, handler);
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

	private void userIsAuthorized(HttpServerRequest request, JsonObject session,
			Handler<Boolean> handler) {
		Binding binding = requestBinding(request);
		if (ActionType.WORKFLOW.equals(binding.getActionType())) {
			authorizeWorkflowAction(session, binding, handler);
		} else if (ActionType.RESOURCE.equals(binding.getActionType())) {
			authorizeResourceAction(request, session, binding, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeResourceAction(HttpServerRequest request, JsonObject session,
			Binding binding, Handler<Boolean> handler) {
		UserInfos user = sessionToUserInfos(session);
		if (user != null && provider != null) {
			provider.authorize(request, binding, user, handler);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeWorkflowAction(JsonObject session, Binding binding,
			Handler<Boolean> handler) {
		JsonArray actions = session.getArray("authorizedActions");
		if (binding != null && binding.getServiceMethod() != null
				&& actions != null && actions.size() > 0) {
			for (Object a: actions) {
				JsonObject action = (JsonObject) a;
				if (binding.getServiceMethod().equals(action.getString("name"))) {
					handler.handle(true);
					return;
				}
			}
		}
		handler.handle(false);
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

	private UserInfos sessionToUserInfos(JsonObject session) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(session.encode(), UserInfos.class);
		} catch (IOException e) {
			return null;
		}
	}

}
