package edu.one.core.registry;

import edu.one.core.infra.Controller;
import edu.one.core.registry.service.AppRegistryService;

public class AppRegistry extends Controller {

	@Override
	public void start() {
		super.start();

		AppRegistryService service = new AppRegistryService(vertx, container, rm);

		service.get("/test/invoke", "testExecute");
	}

}
