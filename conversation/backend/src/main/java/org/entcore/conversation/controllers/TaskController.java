package org.entcore.conversation.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.conversation.cron.PurgeMessages;
import org.entcore.conversation.service.impl.DeleteOrphan;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final DeleteOrphan deleteOrphan;
	private final PurgeMessages purgeMessages;

	public TaskController(DeleteOrphan deleteOrphan, PurgeMessages purgeMessages) {
		this.deleteOrphan = deleteOrphan;
		this.purgeMessages = purgeMessages;
	}

	@Post("api/internal/purge/orphans")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void deleteOrphans(final HttpServerRequest request) {
		log.info("Triggered delete orphan task");
		deleteOrphan.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/purge/messages")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void purgeMessages(final HttpServerRequest request) {
		log.info("Triggered purge old messages task");
		this.purgeMessages.handle(0L);
		render(request, null, 202);
	}
}
