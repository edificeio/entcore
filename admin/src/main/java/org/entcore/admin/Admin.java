package org.entcore.admin;

import org.entcore.admin.controllers.AdminController;
import org.entcore.common.http.BaseServer;

public class Admin extends BaseServer {

	 @Override
	 public void start() {
		 super.start();

		 addController(new AdminController());
	 }

}
