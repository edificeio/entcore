/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.cas.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

import java.util.Arrays;

import fr.wseduc.webutils.http.Renders;
import org.entcore.cas.services.RegisteredServices;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


public class ConfigurationController extends BaseController {

	private RegisteredServices services;

	@Get("/configuration/reload")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void reloadPatterns(HttpServerRequest request) {
		loadPatterns();
		Renders.renderJson(request, new JsonObject().put("result", "done"), 200);
	}

	public void loadPatterns() {
		eb.send("wse.app.registry.bus", new JsonObject().put("action", "list-cas-connectors"),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					services.cleanPatterns();
					JsonArray externalApps = event.body().getJsonArray("result");
					for (Object o: externalApps) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						String service = j.getString("service");
						JsonArray patterns = j.getJsonArray("patterns");
						if (service != null && !service.trim().isEmpty() && patterns != null && patterns.size() > 0) {
							services.addPatterns(service,Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class));
						}
					}
				} else {
					log.error(event.body().getString("message"));
				}
			}
		}));
	}

	@BusAddress(value = "cas.configuration", local = false)
	public void cas(Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "list-services" :
				message.reply(new JsonObject().put("status", "ok")
						.put("result", services.getInfos(message.body().getString("accept-language", "fr"))));
				break;
			case "add-patterns" :
				String service = message.body().getString("service");
				JsonArray patterns = message.body().getJsonArray("patterns");
				message.reply(new JsonObject().put("status",
						services.addPatterns(service, Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class)) ? "ok" : "error"));
				break;
			default:
				message.reply(new JsonObject().put("status", "error").put("message", "invalid.action"));
		}
	}

	public void setRegisteredServices(RegisteredServices services) {
		this.services = services;
	}

}
