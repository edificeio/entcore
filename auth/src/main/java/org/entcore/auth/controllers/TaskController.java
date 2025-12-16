package org.entcore.auth.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.auth.users.NewDeviceWarningTask;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

public class TaskController extends BaseController {

	private final NewDeviceWarningTask NDWTask;

	public TaskController(NewDeviceWarningTask NDWTask) {
		this.NDWTask = NDWTask;
	}

	@Delete("check/new-device-warning")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	@MfaProtected()
	public void newDeviceWarningCheck(final HttpServerRequest request) {
		try {
			NDWTask.handle(0L);
			render(request, null, 202);
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			renderError(request, new JsonObject(e.getMessage()));
		}
	}
}
