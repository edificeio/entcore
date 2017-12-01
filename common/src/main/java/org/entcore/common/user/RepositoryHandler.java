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

package org.entcore.common.user;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class RepositoryHandler implements Handler<Message<JsonObject>> {

	private RepositoryEvents repositoryEvents;
	private final EventBus eb;

	public RepositoryHandler(EventBus eb) {
		this.eb = eb;
		this.repositoryEvents = new LogRepositoryEvents();
	}

	public RepositoryHandler(RepositoryEvents repositoryEvents, EventBus eb) {
		this.eb = eb;
		this.repositoryEvents = repositoryEvents;
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		switch (action) {
			case "export" :
				final String exportId = message.body().getString("exportId", "");
				String userId = message.body().getString("userId", "");
				String path = message.body().getString("path", "");
				final String locale = message.body().getString("locale", "fr");
				final String host = message.body().getString("host", "");
				JsonArray groupIds = message.body().getJsonArray("groups", new JsonArray());
				repositoryEvents.exportResources(exportId, userId, groupIds, path, locale, host, new Handler<Boolean>() {
					@Override
					public void handle(Boolean isExported) {
						JsonObject exported = new JsonObject()
								.put("action", "exported")
								.put("status", (isExported ? "ok" : "error"))
								.put("exportId", exportId)
								.put("locale", locale)
								.put("host", host);
						eb.publish("entcore.export", exported);
					}
				});
				break;
			case "delete-groups" :
				JsonArray groups = message.body().getJsonArray("old-groups", new JsonArray());
				repositoryEvents.deleteGroups(groups);
				break;
			case "delete-users" :
				JsonArray users = message.body().getJsonArray("old-users", new JsonArray());
				repositoryEvents.deleteUsers(users);
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	public void setRepositoryEvents(RepositoryEvents repositoryEvents) {
		this.repositoryEvents = repositoryEvents;
	}

}
