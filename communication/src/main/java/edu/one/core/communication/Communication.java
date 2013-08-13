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

		controller.post("/groups/profils", "setGroupsProfilsMatrix");

		controller.post("/groups/parents/enfants", "setParentEnfantCommunication");

		controller.get("/visible/:userId", "visibleUsers");

		controller.get("/groups/profils", "listVisiblesGroupsProfil");

		controller.get("/groups/classes/enfants", "listVisiblesClassesEnfants");

		controller.get("/profils", "listProfils");

		controller.get("/schools", "listVisiblesSchools");

		try {
			controller.registerMethod(config.getString("address") + ".users", "visibleUsers");
			controller.registerMethod(config.getString("address") + ".schools", "listVisiblesSchools");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

	}

}
