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

package org.entcore.cas.data;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.entcore.cas.services.RegisteredService;
import org.entcore.cas.services.RegisteredServices;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.cas.data.DataHandler;
import fr.wseduc.cas.data.DataHandlerFactory;
import fr.wseduc.cas.http.Request;

public class EntCoreDataHandlerFactory implements DataHandlerFactory {

	private final EventBus eb;
	private static final Logger log = LoggerFactory.getLogger(EntCoreDataHandlerFactory.class);

	private static final String CONF_SERVICES = "services";
	private static final String CONF_SERVICE_CLASS = "class";
	private final RegisteredServices services = new RegisteredServices();

	public EntCoreDataHandlerFactory(EventBus eb, JsonObject conf) {
		this.eb = eb;

		JsonArray confServices = conf.getJsonArray(CONF_SERVICES, new fr.wseduc.webutils.collections.JsonArray());
		for (Object confObject : confServices) {
			try {
				Map<String, Object> confService = ((JsonObject) confObject).getMap();
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

	public RegisteredServices getServices() {
		return services;
	}

}
