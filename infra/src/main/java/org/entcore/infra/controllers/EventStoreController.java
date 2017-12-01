/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.infra.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.infra.services.EventStoreService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.voidResponseHandler;

public class EventStoreController extends BaseController {

	private EventStoreService eventStoreService;
	private JsonArray userBlackList;

	public EventStoreController (JsonObject eventConfig) {
        this.userBlackList = eventConfig.getJsonArray("user-blacklist", new JsonArray());
	}

	@Post("/event/store")
	@SecuredAction("event.store")
	public void store(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
			    if (!authorizedUser(event)) {
                    Renders.ok(request);
                } else {
                    eventStoreService.store(event, voidResponseHandler(request));
                }
			}
		});
	}

	@Post("/event/localhost/store")
	public void storeLocalhost(final HttpServerRequest request) {
		if (("localhost:"+ config.getInteger("port", 8001))
				.equalsIgnoreCase(request.headers().get("Host"))) {
			store(request);
		} else {
			forbidden(request, "invalid.host");
		}
	}

	@BusAddress("event.store")
	public void eventStore(final Message<JsonObject> message) {
		if (!authorizedUser(message.body())) {
			message.reply(new JsonObject().put("status", "ok"));
		} else {
			eventStoreService.store(message.body(), new Handler<Either<String, Void>>() {
				@Override
				public void handle(Either<String, Void> event) {
					if (event.isRight()) {
						message.reply(new JsonObject().put("status", "ok"));
					} else {
						message.reply(new JsonObject().put("status", "error")
								.put("message", event.left().getValue()));
					}
				}
			});
		}
	}

	@BusAddress("event.blacklist")
	public void getBlacklist(final Message<Void> message) {
		message.reply(this.userBlackList);
	}

	public void setEventStoreService(EventStoreService eventStoreService) {
		this.eventStoreService = eventStoreService;
	}

	private boolean authorizedUser (JsonObject event) {
		return !this.userBlackList.contains(event.getString("userId"));
	}

}
