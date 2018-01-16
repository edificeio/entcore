package org.entcore.admin.controllers;

import org.entcore.admin.filters.AdminStructureFilter;
import org.entcore.admin.services.AdminService;
import org.entcore.admin.services.impl.AdminNeoService;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

public class AdminController extends BaseController {

	private final AdminService service = new AdminNeoService();

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

	@Get("api/structure/:id/quicksearch/users")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminStructureFilter.class)
	public void quickSearchUsers(HttpServerRequest request) {
		String structureId = request.params().get("id");
		String input = request.params().get("input");

		if(input == null || input.trim().length() == 0){
			badRequest(request);
			return;
		}

		this.service.quickSearchUsers(structureId, input, arrayResponseHandler(request));
	}

	@Get("api/structure/:id/users")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminStructureFilter.class)
	public void userList(HttpServerRequest request) {
		String structureId = request.params().get("id");
		this.service.userList(structureId, arrayResponseHandler(request));
	}
}
