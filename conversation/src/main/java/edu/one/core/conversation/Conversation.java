package edu.one.core.conversation;

import edu.one.core.common.appregistry.AppRegistryEventsHandler;
import edu.one.core.conversation.controllers.ConversationController;
import edu.one.core.conversation.service.impl.ConversationServiceManager;
import edu.one.core.infra.Server;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

public class Conversation extends Server {

	@Override
	public void start() {
		super.start();

		final ConversationController conversationController =
				new ConversationController(vertx, container, rm, securedActions);

		conversationController.get("conversation", "view");

		new AppRegistryEventsHandler(vertx, new ConversationServiceManager(vertx));

	}

}
