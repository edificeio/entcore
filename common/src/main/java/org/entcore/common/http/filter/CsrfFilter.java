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

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import static fr.wseduc.webutils.Utils.isEmpty;

public class CsrfFilter implements Filter {

	private static final List<String> securedMethods = Arrays.asList("POST", "PUT", "DELETE");
	private final EventBus eb;

	public CsrfFilter(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		if (request instanceof SecureHttpServerRequest && securedMethods.contains(request.method()) &&
				isEmpty(((SecureHttpServerRequest) request).getAttribute("client_id"))) {
			compareToken(request, handler);
		} else {
			handler.handle(true);
		}
	}

	protected void compareToken(final HttpServerRequest request, final Handler<Boolean> handler) {
		UserUtils.getSession(eb, request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				handler.handle(!(session == null || session.getObject("cache") == null ||
						session.getObject("cache").getString("xsrf-token") == null ||
						!session.getObject("cache").getString("xsrf-token").equals(request.headers().get("X-XSRF-TOKEN"))));
			}
		});
	}

	@Override
	public void deny(HttpServerRequest request) {
		Renders.forbidden(request, "invalid.xsrf.token");
	}

}
