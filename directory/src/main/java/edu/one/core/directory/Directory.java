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
				.get("/api/groupes", "groups")
				.get("/api/classes", "classes")
				.get("/api/personnes", "people")
				.get("/api/membres", "members")
				.get("/api/details", "details")
				.get("/api/enseignants", "teachers")
				.get("/api/link", "link")
				.get("/api/create-user", "createUser")
				.get("/api/create-admin", "createAdmin")
				.get("/api/create-group", "createGroup")
				.get("/api/create-school", "createSchool")
				.get("/api/export", "export")
				.post("/api/group-profil", "groupProfile");
		try {
			directoryController.registerMethod("directory", "directoryHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.addFilter(new ActionFilter(directoryController.securedUriBinding(), Server.getEventBus(vertx)));
	}
}
