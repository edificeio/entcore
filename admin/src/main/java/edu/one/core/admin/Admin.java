package edu.one.core.admin;

import edu.one.core.admin.controllers.AdminController;
import edu.one.core.infra.Server;
import edu.one.core.infra.request.filter.ActionFilter;
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