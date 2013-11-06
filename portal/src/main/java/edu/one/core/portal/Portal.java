package edu.one.core.portal;

import edu.one.core.infra.Server;
import edu.one.core.portal.service.PortalService;

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
				.getWithRegEx("/assets/.+", "assets");

	}

}
