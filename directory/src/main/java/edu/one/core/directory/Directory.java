package edu.one.core.directory;

import edu.one.core.directory.controllers.DirectoryController;
import edu.one.core.infra.Server;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;

public class Directory extends Server {

	@Override
	public void start() {
		super.start();
		DirectoryController directoryController = new DirectoryController(vertx, container, rm, securedActions, config);

		directoryController.createSuperAdmin();
		directoryController.get("/admin", "directory")
				.get("/testbe1d", "testBe1d")
				.get("/api/ecole", "school")
				.get("/api/classes", "classes")
				.get("/api/personnes", "people")
				.get("/api/details", "details")
				.post("/api/user", "createUser")
				.get("/api/export", "export")
				.post("/api/group-profil", "groupProfile");
		try {
			directoryController.registerMethod("directory", "directoryHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.addFilter(new ActionFilter(directoryController.securedUriBinding(), getEventBus(vertx)));
	}
}
