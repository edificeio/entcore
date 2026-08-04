package org.entcore.conversation.cron;

import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.conversation.service.ConversationService;

/**
 * Tâche de purge des lignes du garde-fou anti-spam du message d'absence.
 * Peut être déclenchée par le CRON vertx (hors-Kube) ou par l'endpoint
 * api/internal/purge/absence-replies du TaskController (job Kube).
 */
public class PurgeAbsenceReplies implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(PurgeAbsenceReplies.class);

	private final ConversationService conversationService;

	public PurgeAbsenceReplies(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@Override
	public void handle(Long event) {
		log.info("Starting absence replies purge process");
		conversationService.purgeAbsenceReplies()
			.onSuccess(v -> log.info("Absence replies purge process completed successfully"))
			.onFailure(err -> log.error("Absence replies purge process failed: " + err.getMessage(), err));
	}
}
