package org.entcore.common.appregistry;


import org.vertx.java.core.json.JsonArray;

public interface AppRegistryEventsService {

	void authorizedActionsUpdated();

	void userGroupUpdated(JsonArray users);

}
