/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
		JsonArray infos = new fr.wseduc.webutils.collections.JsonArray();
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
