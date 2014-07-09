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

package org.entcore.conversation;

import org.entcore.common.appregistry.AppRegistryEventsHandler;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.conversation.controllers.ConversationController;
import org.entcore.conversation.service.impl.ConversationRepositoryEvents;
import org.entcore.conversation.service.impl.ConversationServiceManager;
import fr.wseduc.webutils.Server;

public class Conversation extends Server {

	@Override
	public void start() {
		super.start();
		Neo4j.getInstance().init(getEventBus(vertx),
				config.getString("neo4j-address", "wse.neo4j.persistor"));

		vertx.eventBus().registerHandler("user.repository",
				new RepositoryHandler(new ConversationRepositoryEvents()));

		final ConversationController conversationController =
				new ConversationController(vertx, container, rm, securedActions);

		conversationController.get("conversation", "view");
		conversationController.post("draft", "createDraft");
		conversationController.put("draft/:id", "updateDraft");
		conversationController.post("send", "send");
		conversationController.get("list/:folder", "list");
		conversationController.get("count/:folder", "count");
		conversationController.get("visible", "visible");
		conversationController.get("message/:id", "getMessage");
		conversationController.put("trash", "trash");
		conversationController.put("restore", "restore");
		conversationController.delete("delete", "delete");

		try {
			conversationController.registerMethod("org.entcore.conversation", "conversationEventBusHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		new AppRegistryEventsHandler(vertx, new ConversationServiceManager(vertx,
				config.getString("app-name", Conversation.class.getSimpleName())));

	}

}
