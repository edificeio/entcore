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

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Binding;


public class ActionFilter extends AbstractActionFilter {

	private final Vertx vertx;
	private final EventBus eb;

	public ActionFilter(Set<Binding> bindings, Vertx vertx, ResourcesProvider provider) {
		super(bindings, provider);
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
	}

	public ActionFilter(Set<Binding> bindings, Vertx vertx) {
		this(bindings, vertx, null);
	}

	public ActionFilter(List<Set<Binding>> bindings, Vertx vertx, ResourcesProvider provider) {
		super(new HashSet<Binding>(), provider);
		if (bindings != null) {
			for (Set<Binding> bs: bindings) {
				this.bindings.addAll(bs);
			}
		}
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
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
				} else if (request instanceof SecureHttpServerRequest &&
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

}
