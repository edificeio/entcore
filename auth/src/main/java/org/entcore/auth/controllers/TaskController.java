package org.entcore.auth.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.auth.users.NewDeviceWarningTask;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final NewDeviceWarningTask NDWTask;

	public TaskController(NewDeviceWarningTask NDWTask) {
		this.NDWTask = NDWTask;
	}

	@Post("api/internal/check/new-device-warning")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void newDeviceWarningCheck(final HttpServerRequest request) {
		log.info("Triggered new device warning check task");
		NDWTask.handle(0L);
		render(request, null, 202);
	}
}
