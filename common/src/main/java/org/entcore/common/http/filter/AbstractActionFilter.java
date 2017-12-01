/*
 * Copyright © WebServices pour l'Éducation, 2017
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
 */

package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static org.entcore.common.utils.StringUtils.isEmpty;

public abstract class AbstractActionFilter implements Filter {

	protected static final List<String> authorizationTypes = Arrays.asList("Basic", "Bearer");
	protected final Set<Binding> bindings;
	protected final ResourcesProvider provider;

	public AbstractActionFilter(Set<Binding> bindings, ResourcesProvider provider) {
		this.bindings = bindings;
		this.provider = provider;
	}

	protected void userIsAuthorized(HttpServerRequest request, JsonObject session,
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
		JsonArray actions = session.getJsonArray("authorizedActions");
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
		if (session.getJsonObject("functions", new JsonObject()).containsKey("SUPER_ADMIN")) {
			handler.handle(true);
			return;
		}
		handler.handle(false);
	}

	private Binding requestBinding(HttpServerRequest request) {
		for (Binding binding: bindings) {
			if (!request.method().name().equals(binding.getMethod().name())) {
				continue;
			}
			Matcher m = binding.getUriPattern().matcher(request.path());
			if (m.matches()) {
				return binding;
			}
		}
		return null;
	}

	protected void clientIsAuthorizedByScope(SecureHttpServerRequest request, Handler<Boolean> handler) {
		final String authorizationType = request.getAttribute("authorization_type");
		if (isEmpty(authorizationType) || !authorizationTypes.contains(authorizationType)) {
			handler.handle(false);
			return;
		}
		String scope = request.getAttribute("scope");
		Binding b = requestBinding(request);
		handler.handle(scope.contains(b.getServiceMethod()));
	}

}
