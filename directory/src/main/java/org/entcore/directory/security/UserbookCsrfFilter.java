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

package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.http.filter.CsrfFilter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import java.util.Set;

public class UserbookCsrfFilter extends CsrfFilter {

	public UserbookCsrfFilter(EventBus eb, Set<Binding> bindings) {
		super(eb, bindings);
	}

	@Override
	public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
		if (request instanceof SecureHttpServerRequest && "GET".equals(request.method()) &&
				(request.uri().contains("/userbook/api/edit") || request.uri().contains("/userbook/api/set"))) {
			compareToken(request, handler);
		} else {
			handler.handle(true);
		}
	}

}
