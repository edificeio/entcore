/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.cas.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.entcore.cas.services.RegisteredService;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import fr.wseduc.cas.data.DataHandler;
import fr.wseduc.cas.data.DataHandlerFactory;
import fr.wseduc.cas.http.Request;

public class EntCoreDataHandlerFactory implements DataHandlerFactory {

	private final EventBus eb;
	private static final Logger log = LoggerFactory.getLogger(EntCoreDataHandlerFactory.class);

	private static final String CONF_SERVICES = "services";
	private static final String CONF_SERVICE_CLASS = "class";
	private final List<RegisteredService> services = new ArrayList<RegisteredService>();

	public EntCoreDataHandlerFactory(EventBus eb, JsonObject conf) {
		this.eb = eb;

		JsonArray confServices = conf.getArray(CONF_SERVICES, new JsonArray());
		for (Object confObject : confServices.toList()) {
			try {
				Map<String, Object> confService = (Map<String, Object>) confObject;
				String className = String.valueOf(confService.get(CONF_SERVICE_CLASS));
				if (className != null) {
					RegisteredService service = (RegisteredService) Class.forName(className).newInstance();
					service.configure(eb, confService);
					services.add(service);
				}
			}
			catch (PatternSyntaxException pe) {
				log.error("Invalid Authorized Service pattern", pe);
			}
			catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				log.error("Failed to instantiate Service", e);
			}
		}
	}

	@Override
	public DataHandler create(Request request) {
		EntCoreDataHandler dataHandler = new EntCoreDataHandler(request, eb);
		dataHandler.setServices(services);
		return dataHandler;
	}
}
