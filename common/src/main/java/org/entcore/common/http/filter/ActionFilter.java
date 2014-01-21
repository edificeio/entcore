package org.entcore.common.http.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import edu.one.core.infra.security.SecureHttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import edu.one.core.infra.request.filter.Filter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.http.Binding;
import edu.one.core.infra.security.ActionType;

public class ActionFilter implements Filter {

	private final Set<Binding> bindings;
	private final EventBus eb;
	private final ResourcesProvider provider;
	private final boolean oauthEnabled;

	public ActionFilter(Set<Binding> bindings, EventBus eb, ResourcesProvider provider, boolean oauthEnabled) {
		this.bindings = bindings;
		this.eb = eb;
		this.provider = provider;
		this.oauthEnabled = oauthEnabled;
	}

	public ActionFilter(Set<Binding> bindings, EventBus eb, ResourcesProvider provider) {
		this(bindings, eb, provider, false);
	}

	public ActionFilter(Set<Binding> bindings, EventBus eb) {
		this(bindings, eb, null);
	}

	public ActionFilter(List<Set<Binding>> bindings, EventBus eb, ResourcesProvider provider, boolean oauthEnabled) {
		Set<Binding> b = new HashSet<>();
		if (bindings != null) {
			for (Set<Binding> bs: bindings) {
				b.addAll(bs);
			}
		}
		this.bindings = b;
		this.eb = eb;
		this.provider = provider;
		this.oauthEnabled = oauthEnabled;
	}

	public ActionFilter(List<Set<Binding>> bindings, EventBus eb, ResourcesProvider provider) {
		this(bindings, eb, provider, false);
	}

	public ActionFilter(List<Set<Binding>> bindings, EventBus eb) {
		this(bindings, eb, null);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject session) {
				if (session != null) {
					userIsAuthorized(request, session, handler);
				} else if (oauthEnabled && request instanceof SecureHttpServerRequest &&
						((SecureHttpServerRequest) request).getAttribute("client_id") != null) {
					clientIsAuthorizedByScope((SecureHttpServerRequest) request, handler);
				} else {
					handler.handle(false);
				}
			}
		});
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
		} else if (ActionType.AUTHENTICATED.equals(binding.getActionType())) {
			handler.handle(true);
		} else {
			handler.handle(false);
		}
	}

	private void authorizeResourceAction(HttpServerRequest request, JsonObject session,
										 Binding binding, Handler<Boolean> handler) {
		UserInfos user = UserUtils.sessionToUserInfos(session);
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
		if ("SuperAdmin".equals(session.getString("type"))) {
			handler.handle(true);
			return;
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

	private void clientIsAuthorizedByScope(SecureHttpServerRequest request, Handler<Boolean> handler) {
		String scope = request.getAttribute("scope");
		Binding b = requestBinding(request);
		handler.handle(scope.contains(b.getServiceMethod()));
	}

}
