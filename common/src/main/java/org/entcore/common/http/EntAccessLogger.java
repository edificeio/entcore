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

package org.entcore.common.http;

import fr.wseduc.webutils.request.AccessLogger;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class EntAccessLogger extends AccessLogger {

	private final EventBus eb;

	public EntAccessLogger(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void log(final HttpServerRequest request, final Handler<Void> handler) {
		request.pause();
		UserUtils.getSession(eb, request, true, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject session) {
				if (session != null) {
					log.trace(formatLog(request) + " - " + session.getString("userId"));
				} else {
					log.trace(formatLog(request));
				}
				request.resume();
				handler.handle(null);
			}
		});
	}

}
