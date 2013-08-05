package edu.one.core.communication;

import edu.one.core.communication.controllers.CommunicationController;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Server;

public class Communication extends Server {

	@Override
	public void start() {
		super.start();

		Controller controller = new CommunicationController(vertx, container, rm, securedActions);

		controller.get("/communication", "view");

		controller.post("/communication/profils", "setGroupProfilsCommunication");

		controller.get("/visible/:userId", "visibleUsers");

		controller.get("/groups/profils", "listVisiblesGroupsProfil");

		controller.get("/profils", "listProfils");

		try {
			controller.registerMethod(config.getString("address"), "visibleUsers");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

	}

}
