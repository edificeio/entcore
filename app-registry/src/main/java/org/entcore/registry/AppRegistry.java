package org.entcore.registry;

import edu.one.core.infra.Server;
import org.entcore.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import org.entcore.registry.service.AppRegistryService;

public class AppRegistry extends Server {

	@Override
	public void start() {
		super.start();

		AppRegistryService service = new AppRegistryService(vertx, container, rm, securedActions);
		try {
			service.registerMethod(config.getString("address"), "collectApps");
			service.registerMethod(config.getString("address") + ".applications", "applications");
			service.registerMethod(config.getString("address") + ".bus", "registryEventBusHandler");
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

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(), Server.getEventBus(vertx)));

	}

}
