package org.entcore.conversation.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.conversation.cron.PurgeAbsenceReplies;
import org.entcore.conversation.service.impl.DeleteOrphan;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final DeleteOrphan deleteOrphan;
	private final PurgeAbsenceReplies purgeAbsenceReplies;

	public TaskController(DeleteOrphan deleteOrphan, PurgeAbsenceReplies purgeAbsenceReplies) {
		this.deleteOrphan = deleteOrphan;
		this.purgeAbsenceReplies = purgeAbsenceReplies;
	}

	@Post("api/internal/purge/orphans")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void deleteOrphans(final HttpServerRequest request) {
		log.info("Triggered delete orphan task");
		deleteOrphan.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/purge/absence-replies")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void purgeAbsenceReplies(final HttpServerRequest request) {
		log.info("Triggered purge absence replies task");
		this.purgeAbsenceReplies.handle(0L);
		render(request, null, 202);
	}
}
