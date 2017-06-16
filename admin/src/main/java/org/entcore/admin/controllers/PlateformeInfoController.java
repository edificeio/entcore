package org.entcore.admin.controllers;

import org.entcore.admin.filters.AdminStructureFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

public class PlateformeInfoController extends BaseController {

	private boolean smsActivated;

	@Get("api/plateforme/module/sms")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	@ResourceFilter(AdminStructureFilter.class)
	public void moduleSms(HttpServerRequest request) {
		renderJson(request, new JsonObject().putBoolean("activated", this.smsActivated), 200);
	}
	
	public boolean isSmsModule() {
		return smsActivated;
	}

	public void setSmsModule(boolean smsModule) {
		this.smsActivated = smsModule;
	}
}
