package org.entcore.conversation.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.conversation.service.impl.DeleteOrphan;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final DeleteOrphan deleteOrphan;

	public TaskController(DeleteOrphan deleteOrphan) {
		this.deleteOrphan = deleteOrphan;
	}

	@Post("api/internal/purge/orphans")
	public void deleteOrphans(final HttpServerRequest request) {
		log.info("Triggered delete orphan task");
		deleteOrphan.handle(0L);
		render(request, null, 202);
	}
}
