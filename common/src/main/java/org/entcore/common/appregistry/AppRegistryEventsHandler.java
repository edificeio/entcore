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
		vertx.eventBus().registerHandler(APP_REGISTRY_PUBLISH_ADDRESS, this);
	}

	@Override
	public void handle(Message<JsonObject> event) {
		String type = event.body().getString("type");
		if (type != null) {
			switch (type) {
				case PROFILE_GROUP_ACTIONS_UPDATED:
					appRegistryEventsService.authorizedActionsUpdated();
					break;
			}
		}
	}

}
