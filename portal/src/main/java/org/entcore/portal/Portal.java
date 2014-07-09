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

package org.entcore.portal;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import org.entcore.common.http.filter.ActionFilter;
import org.entcore.portal.service.PortalService;

public class Portal extends Server {

	@Override
	public void start() {
		super.start();

		PortalService service = new PortalService(vertx, container, rm, securedActions);

		service.get("/", "portal")
				.get("/welcome", "welcome")
				.get("/applications-list", "applicationsList")
				.get("/adapter", "adapter")
				.get("/theme", "getTheme")
				.get("/skin", "getSkin")
				.get("/locale", "locale")
				.get("/admin", "admin")
				.get("/admin-urls", "adminURLS")
				.get("/resources-applications", "resourcesApplications")
				.get("/widgets", "widgets")
				.get("/themes", "themes")
				.getWithRegEx("/assets/.+", "assets");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(), getEventBus(vertx)));
	}

}
