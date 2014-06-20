/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.user;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;


public class RepositoryHandler implements Handler<Message<JsonObject>> {

	private RepositoryEvents repositoryEvents;

	public RepositoryHandler() {
		this.repositoryEvents = new LogRepositoryEvents();
	}

	public RepositoryHandler(RepositoryEvents repositoryEvents) {
		this.repositoryEvents = repositoryEvents;
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "export" :
				String exportId = message.body().getString("exportId", "");
				String userId = message.body().getString("userId", "");
				String path = message.body().getString("path", "");
				String locale = message.body().getString("locale", "fr");
				JsonArray groupIds = message.body().getArray("groups", new JsonArray());
				repositoryEvents.exportResources(exportId, userId, groupIds, path, locale);
				break;
			case "delete-groups" :
				JsonArray groups = message.body().getArray("old-groups", new JsonArray());
				repositoryEvents.deleteGroups(groups);
				break;
			case "delete-users" :
				JsonArray users = message.body().getArray("old-users", new JsonArray());
				repositoryEvents.deleteUsers(users);
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	public void setRepositoryEvents(RepositoryEvents repositoryEvents) {
		this.repositoryEvents = repositoryEvents;
	}

}
