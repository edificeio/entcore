package org.entcore.infra.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.infra.cron.HardBounceTask;

public class TaskController extends BaseController {

	private final HardBounceTask hardBounceTask;

	public TaskController(HardBounceTask hardBounceTask) {
		this.hardBounceTask = hardBounceTask;
	}

	@Delete("clean/hard-bounces")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	@MfaProtected()
	public void cleanHardBounceEmails(final HttpServerRequest request) {
		try {
			hardBounceTask.handle(0L);
			render(request, null, 202);
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			renderError(request, new JsonObject(e.getMessage()));
		}
	}
}
