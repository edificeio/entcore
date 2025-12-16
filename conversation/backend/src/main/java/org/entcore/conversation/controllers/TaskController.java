package org.entcore.conversation.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.conversation.service.impl.DeleteOrphan;

public class TaskController extends BaseController {

	private final DeleteOrphan deleteOrphan;

	public TaskController(DeleteOrphan deleteOrphan) {
		this.deleteOrphan = deleteOrphan;
	}

	@Delete("api/purge/orphans")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	@MfaProtected()
	public void deleteOrphans(final HttpServerRequest request) {
		try {
			deleteOrphan.handle(0L);
			render(request, null, 202);
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			renderError(request, new JsonObject(e.getMessage()));
		}
	}
}
