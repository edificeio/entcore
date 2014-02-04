package org.entcore.communication;

import org.entcore.communication.controllers.CommunicationController;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Server;
import org.entcore.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;

public class Communication extends Server {

	@Override
	public void start() {
		super.start();

		Controller controller = new CommunicationController(vertx, container, rm, securedActions);

		controller.get("/admin", "view");

		controller.post("/groups/profils", "setGroupsProfilsMatrix");

		controller.post("/groups/parents/enfants", "setParentEnfantCommunication");

		controller.get("/visible/:userId", "visibleUsers");

		controller.get("/groups/profils", "listVisiblesGroupsProfil");

		controller.get("/groups/classes/enfants", "listVisiblesClassesEnfants");

		controller.get("/profils", "listProfils");

		controller.get("/schools", "listVisiblesSchools");

		controller.put("/rules/:schoolId", "defaultCommunicationRules");

		controller.delete("/rules/:schoolId", "removeCommunicationRules");

		try {
			controller.registerMethod(config.getString("address") + ".users", "visibleUsers");
			controller.registerMethod(config.getString("address") + ".schools", "listVisiblesSchools");
			controller.registerMethod(config.getString("address"), "communicationEventBusHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.addFilter(new ActionFilter(controller.securedUriBinding(), getEventBus(vertx)));

	}

}
