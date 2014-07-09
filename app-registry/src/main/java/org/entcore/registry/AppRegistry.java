/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.registry;

import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;
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
		service.get("/static-admin", "staticAdmin");
		service.get("/app-preview", "appPreview");

		service.get("/applications", "listApplications");

		service.get("/application/:name", "listApplicationActions");

		service.get("/applications/actions", "listApplicationsWithActions");

		service.post("/role", "createRole");

		service.get("/roles", "listRoles");

		service.get("/roles/actions", "listRolesWithActions");

		service.get("/groups", "listGroups");

		service.get("/groups/roles", "listGroupsWithRoles");

		service.post("/authorize/group", "linkGroup");

		service.get("/schools", "listStructures");

		service.post("/application/conf", "applicationConf");

		service.get("/application/conf/:id", "application");

		service.post("/application/external", "createExternalApp");

		service.put("/application", "recordApplication");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(), Server.getEventBus(vertx)));

	}

}
