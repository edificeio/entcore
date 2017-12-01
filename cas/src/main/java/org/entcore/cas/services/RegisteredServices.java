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

package org.entcore.cas.services;

import java.util.LinkedHashSet;
import java.util.Set;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.ServiceTicket;
import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RegisteredServices {

	private final Set<RegisteredService> services = new LinkedHashSet<>();
	private static final Logger log = LoggerFactory.getLogger(RegisteredServices.class);

	public void add(RegisteredService service) {
		services.add(service);
	}

	public RegisteredService matches(String service) {
		for (RegisteredService registeredService : services) {
			if (registeredService.matches(service)) {
				if (log.isDebugEnabled()) log.debug("service + |" + service + "| matches with registered service : " + registeredService.getClass().getSimpleName());
				return registeredService;
			}
		}
		return null;
	}

	public void getUser(String userId, String service, Handler<User> userHandler) {
		RegisteredService registeredService = matches(service);
		if (registeredService != null) {
			registeredService.getUser(userId, service, userHandler);
		} else {
			userHandler.handle(null);
		}
	}

	public String formatService(String service, ServiceTicket st) {
		RegisteredService registeredService = matches(service);
		if (registeredService != null) {
			return registeredService.formatService(service, st);
		}
		return null;
	}

	public JsonArray getInfos(String acceptLanguage) {
		JsonArray infos = new JsonArray();
		for (RegisteredService registeredService: services) {
			infos.add(registeredService.getInfos(acceptLanguage));
		}
		return infos;
	}

	public boolean addPatterns(String service, String... patterns) {
		if (service != null && !service.trim().isEmpty() && patterns != null && patterns.length > 0) {
			for (RegisteredService registeredService: services) {
				if (service.equals(registeredService.getId())) {
					registeredService.addPatterns(patterns);
					return true;
				}
			}
		}
		return false;
	}

	public void cleanPatterns() {
		for (RegisteredService registeredService: services) {
			registeredService.cleanPatterns();
		}
	}

}
