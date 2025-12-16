package org.entcore.infra.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.infra.cron.HardBounceTask;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final HardBounceTask hardBounceTask;

	public TaskController(HardBounceTask hardBounceTask) {
		this.hardBounceTask = hardBounceTask;
	}

	@Post("/api/internal/clean/hard-bounces")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void cleanHardBounceEmails(final HttpServerRequest request) {
		log.info("Triggered clean hard bounce task");
		hardBounceTask.handle(0L);
		render(request, null, 202);
	}
}
