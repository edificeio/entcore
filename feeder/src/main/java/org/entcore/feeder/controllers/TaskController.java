package org.entcore.feeder.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.entcore.feeder.dictionary.structures.User;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final User.DeleteTask deleteTask;

	public TaskController(User.DeleteTask deleteTask) {
		this.deleteTask = deleteTask;
	}

	@Post("api/internal/delete/users")
	public void deletePreDeletedUsers(final HttpServerRequest request) {
		log.info("Triggered delete pre deleted users task");
		deleteTask.handle(0L);
		render(request, null, 202);
	}




}
