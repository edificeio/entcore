package org.entcore.portal;

import edu.one.core.infra.Server;
import org.entcore.portal.service.PortalService;

public class Portal extends Server {

	@Override
	public void start() {
		super.start();

		PortalService service = new PortalService(vertx, container, rm, securedActions);

		service.get("/", "portal")
				.get("/theme-documentation", "themeDocumentation")
				.get("/apps", "apps")
				.get("/adapter", "adapter")
				.get("/theme", "getTheme")
				.get("/locale", "locale")
				.getWithRegEx("/assets/.+", "assets");

	}

}
