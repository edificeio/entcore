package org.entcore.common.appregistry;


import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface AppRegistryEventsService {

	void authorizedActionsUpdated(JsonArray groups);

	void userGroupUpdated(JsonArray users, Message<JsonObject> message);

}
