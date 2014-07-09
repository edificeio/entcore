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

package org.entcore.communication;

import org.entcore.communication.controllers.CommunicationController;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;

public class Communication extends Server {

	@Override
	public void start() {
		super.start();

		Controller controller = new CommunicationController(vertx, container, rm, securedActions);

		controller.get("/admin", "view");
		controller.get("/static-view", "staticView");

		controller.post("/groups/profils", "setGroupsProfilsMatrix");

		controller.post("/groups/parents/enfants", "setParentEnfantCommunication");

		controller.get("/visible/:userId", "visibleUsers");

		controller.get("/groups/profils", "listVisiblesGroupsProfil");

		controller.get("/groups/classes/enfants", "listVisiblesClassesEnfants");

		controller.get("/profils", "listProfils");

		controller.get("/schools", "listVisiblesStructures");

		controller.put("/rules/:schoolId", "defaultCommunicationRules");

		controller.delete("/rules/:schoolId", "removeCommunicationRules");

		try {
			controller.registerMethod(config.getString("address") + ".users", "visibleUsers");
			controller.registerMethod(config.getString("address") + ".schools", "listVisiblesStructures");
			controller.registerMethod(config.getString("address"), "communicationEventBusHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.addFilter(new ActionFilter(controller.securedUriBinding(), getEventBus(vertx)));

	}

}
