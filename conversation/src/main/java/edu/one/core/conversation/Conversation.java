package edu.one.core.conversation;

import edu.one.core.conversation.controllers.ConversationController;
import edu.one.core.infra.Server;

public class Conversation extends Server {

	@Override
	public void start() {
		super.start();

		ConversationController conversationController =
				new ConversationController(vertx, container, rm, securedActions);

		conversationController.get("conversation", "view");
	}

}
