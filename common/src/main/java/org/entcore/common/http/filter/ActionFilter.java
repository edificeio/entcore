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

package org.entcore.common.http.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import fr.wseduc.webutils.request.filter.Filter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.security.ActionType;

public class ActionFilter implements Filter {

	private final Set<Binding> bindings;
	private final Vertx vertx;
	private final EventBus eb;
	private final ResourcesProvider provider;
	private final boolean oauthEnabled;

	public ActionFilter(Set<Binding> bindings, Vertx vertx, ResourcesProvider provider, boolean oauthEnabled) {
		this.bindings = bindings;
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
		this.provider = provider;
		this.oauthEnabled = oauthEnabled;
	}

	public ActionFilter(Set<Binding> bindings, Vertx vertx, ResourcesProvider provider) {
		this(bindings, vertx, provider, false);
	}

	public ActionFilter(Set<Binding> bindings, Vertx vertx) {
		this(bindings, vertx, null);
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx, ResourcesProvider provider, boolean oauthEnabled) {
		Set<Binding> b = new HashSet<>();
		if (bindings != null) {
			for (Set<Binding> bs: bindings) {
				b.addAll(bs);
			}
		}
		this.bindings = b;
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
		this.provider = provider;
		this.oauthEnabled = oauthEnabled;
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx, ResourcesProvider provider) {
		this(bindings, vertx, provider, false);
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx) {
		this(bindings, vertx, null);
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
					UserAuthFilter.redirectLogin(vertx, request);
				}
			}
		});
	}

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
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
		if (session.getObject("functions", new JsonObject()).containsField("SUPER_ADMIN")) {
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
