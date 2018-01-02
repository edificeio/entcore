package org.entcore.admin.controllers;

import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

public class AdminController extends BaseController {

	@Get("")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	public void serveHome(HttpServerRequest request) {
		renderView(request, new JsonObject(), "admin.html", null);
	}

	@Get(value = "(?!api).*", regex = true)
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminFilter.class)
	public void serveHomeAlias(HttpServerRequest request) {
		serveHome(request);
	}
}
