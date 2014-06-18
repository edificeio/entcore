/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
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
				.get("/widgets", "widgets")
				.get("/themes", "themes")
				.getWithRegEx("/assets/.+", "assets");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(), getEventBus(vertx)));
	}

}
