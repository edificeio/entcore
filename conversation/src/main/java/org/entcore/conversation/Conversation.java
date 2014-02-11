package org.entcore.conversation;

import org.entcore.common.appregistry.AppRegistryEventsHandler;
import org.entcore.conversation.controllers.ConversationController;
import org.entcore.conversation.service.impl.ConversationServiceManager;
import fr.wseduc.webutils.Server;

public class Conversation extends Server {

	@Override
	public void start() {
		super.start();

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

		new AppRegistryEventsHandler(vertx, new ConversationServiceManager(vertx,
				config.getString("app-name", Conversation.class.getSimpleName())));

	}

}
