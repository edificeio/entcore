package org.entcore.timeline.cron;


import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.timeline.services.FlashMsgService;

public class PurgeMessageCronTask implements Handler<Long> {
	private static final Logger log = LoggerFactory.getLogger(PurgeMessageCronTask.class);
	final FlashMsgService flashMsgService;

	public PurgeMessageCronTask(FlashMsgService flashMsgService) {
		this.flashMsgService = flashMsgService;
	}

	@Override
	public void handle(Long event) {
		flashMsgService.purgeMessagesRead(res -> {
			if (res.isLeft()) {
				log.error("[Timeline - FlashMessages] - Purge of flashmsg.messages_read failed - " + res.left().getValue());
			} else {
				log.info("[Timeline - FlashMessages] - Purge of flashmsg.messages_read succeeded");
			}
		});

	}
}
