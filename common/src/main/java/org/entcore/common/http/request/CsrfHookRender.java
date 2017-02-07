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

package org.entcore.common.http.request;

import fr.wseduc.webutils.http.HookProcess;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

public class CsrfHookRender implements HookProcess {

	private final EventBus eb;

	public CsrfHookRender(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void execute(final HttpServerRequest request, final Handler<Boolean> handler) {
		if (request instanceof SecureHttpServerRequest) {
			final String token = UUID.randomUUID().toString();
			final JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if (session == null) {
				handler.handle(false);
				return;
			}
			final String userId = session.getString("userId");
			UserUtils.addSessionAttribute(eb, userId, "xsrf-token", token, new Handler<Boolean>() {
				@Override
				public void handle(Boolean s) {
					if (Boolean.TRUE.equals(s)) {
						CookieHelper.set("XSRF-TOKEN", token, request);
					}
					handler.handle(s);
				}
			});
		} else {
			handler.handle(true);
		}
	}

}
