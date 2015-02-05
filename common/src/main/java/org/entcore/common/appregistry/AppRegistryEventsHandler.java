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

package org.entcore.common.appregistry;

import static org.entcore.common.appregistry.AppRegistryEvents.*;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public final class AppRegistryEventsHandler implements Handler<Message<JsonObject>> {

	private final AppRegistryEventsService appRegistryEventsService;

	public AppRegistryEventsHandler(Vertx vertx, AppRegistryEventsService service) {
		appRegistryEventsService = service;
		vertx.eventBus().registerLocalHandler(APP_REGISTRY_PUBLISH_ADDRESS, this);
	}

	@Override
	public void handle(Message<JsonObject> event) {
		String type = event.body().getString("type");
		if (type != null) {
			switch (type) {
				case PROFILE_GROUP_ACTIONS_UPDATED:
					appRegistryEventsService.authorizedActionsUpdated(event.body().getArray("groups"));
					break;
				case USER_GROUP_UPDATED:
					appRegistryEventsService.userGroupUpdated(event.body().getArray("users"), event);
					break;
			}
		}
	}

}
