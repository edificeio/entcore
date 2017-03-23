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

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;
import java.util.regex.Matcher;


/**
 * This Filter only use HTTP to retrieve user's info.
 * It targets internal module and uses cookie to authorize user request
 */

public class HttpActionFilter implements Filter {

	private final Set<Binding> bindings;
	private final ResourcesProvider provider;
	private final HttpClient httpClient;
	private final Vertx vertx;

	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx,
			ResourcesProvider provider) {
		this.bindings = bindings;
		this.provider = provider;
		this.vertx = vertx;
		this.httpClient = vertx.createHttpClient()
				.setHost("localhost")
				.setPort(conf.getInteger("entcore.port", 8009))
				.setMaxPoolSize(16);
	}

	public HttpActionFilter(Set<Binding> bindings, JsonObject conf, Vertx vertx) {
		this(bindings, conf, vertx, null);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		request.pause();
		httpClient.get("/auth/oauth2/userinfo", new Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse response) {
				response.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer body) {
						request.resume();
						JsonObject session = new JsonObject(body.toString());
						if (response.statusCode() == 200) {
							if (request instanceof SecureHttpServerRequest) {
								((SecureHttpServerRequest) request).setSession(session);
							}
							userIsAuthorized(request, session, handler);
						} else {
							UserAuthFilter.redirectLogin(vertx, request);
						}
					}
				});
			}
		})
				.putHeader("Cookie", request.headers().get("Cookie"))
				.putHeader("Accept", "application/json; version=2.1")
				.end();
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
		if (session != null && provider != null) {
			UserInfos user = UserUtils.sessionToUserInfos(session);
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

}
