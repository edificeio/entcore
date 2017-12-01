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

package org.entcore.registry;

import fr.wseduc.webutils.collections.JsonObject;
import org.entcore.common.appregistry.AppRegistryEventsHandler;
import org.entcore.common.http.BaseServer;
import org.entcore.registry.controllers.AppRegistryController;
import org.entcore.registry.controllers.ExternalApplicationController;
import org.entcore.registry.controllers.WidgetController;
import org.entcore.registry.filters.AppRegistryFilter;
import org.entcore.registry.services.impl.NopAppRegistryEventService;

public class AppRegistry extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();
		addController(new AppRegistryController());
		addController(new ExternalApplicationController());
		addController(new WidgetController());
		setDefaultResourceFilter(new AppRegistryFilter());
		new AppRegistryEventsHandler(vertx, new NopAppRegistryEventService());
		vertx.eventBus().publish("app-registry.loaded", new JsonObject());
	}

}
