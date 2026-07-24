package org.entcore.conversation.cron;

import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.conversation.service.ConversationService;

/**
 * Tâche cron de purge des anciens messages de la messagerie.
 * Peut être déclenchée par le CRON vertx (hors-Kube) ou par l'endpoint
 * api/internal/purge/messages du TaskController (job Kube).
 */
public class PurgeMessages implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(PurgeMessages.class);

	private final ConversationService conversationService;

	public PurgeMessages(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@Override
	public void handle(Long event) {
		log.info("Starting old messages purge process");
		conversationService.purgeMessages()
			.onSuccess(v -> log.info("Old messages purge process completed successfully"))
			.onFailure(err -> log.error("Old messages purge process failed: " + err.getMessage(), err));
	}
}
