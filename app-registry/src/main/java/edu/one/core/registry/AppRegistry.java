package edu.one.core.registry;

import edu.one.core.infra.Controller;
import edu.one.core.registry.service.AppRegistryService;

public class AppRegistry extends Controller {

	@Override
	public void start() {
		super.start();

		AppRegistryService service = new AppRegistryService(vertx, container, rm);

		try {
			service.registerMethod(config.getString("address"), "collectApps");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		service.get("/applications", "listApplications");

		service.get("/application/:name", "listApplicationActions");
	}

}
