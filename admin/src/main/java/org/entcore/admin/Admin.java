package org.entcore.admin;

import org.entcore.admin.controllers.AdminController;
import edu.one.core.infra.Server;
import org.entcore.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;

public class Admin extends Server {

	@Override
	public void start() {
		super.start();

		AdminController adminController = new AdminController(vertx, container, rm, securedActions);

		adminController.get("", "admin");

		SecurityHandler.addFilter(new ActionFilter(adminController.securedUriBinding(), getEventBus(vertx)));

	}

}