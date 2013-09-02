package edu.one.core.registry;

import edu.one.core.infra.Server;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.registry.service.AppRegistryService;

public class AppRegistry extends Server {

	@Override
	public void start() {
		super.start();

		AppRegistryService service = new AppRegistryService(vertx, container, rm, securedActions);

		try {
			service.registerMethod(config.getString("address"), "collectApps");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		service.get("/admin", "view");

		service.get("/applications", "listApplications");

		service.get("/application/:name", "listApplicationActions");

		service.get("/applications/actions", "listApplicationsWithActions");

		service.post("/role", "createRole");

		service.get("/roles", "listRoles");

		service.get("/roles/actions", "listRolesWithActions");

		service.get("/groups", "listGroups");

		service.get("/groups/roles", "listGroupsWithRoles");

		service.post("/authorize/group", "linkGroup");

		service.get("/schools", "listSchools");

		service.post("/application/conf", "applicationConf");

		service.get("/application/conf/:id", "application");

		service.post("/application/external", "createExternalApp");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(), vertx.eventBus()));

	}

}
