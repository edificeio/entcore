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
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;

import java.util.Arrays;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.cas.mapping.MappingService;
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
	private final MappingService mappingService = MappingService.getInstance();

	@Get("/configuration/reload")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void reloadPatterns(HttpServerRequest request) {
		loadPatterns();
		Renders.renderJson(request, new JsonObject().put("result", "done"), 200);
	}

	@Get("/configuration/mappings")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getMappings(HttpServerRequest request) {
		mappingService.getMappings().setHandler(res->{
			if(res.succeeded()){
				Renders.renderJson(request, res.result().toJson());
			}else{
				Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantload"));
				log.error("Failed to load mapping : ", res.cause());
			}
		});
	}

	@Get("/configuration/mappings/reload")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void reloadMapping(HttpServerRequest request) {
		mappingService.reset();
		getMappings(request);
	}

	@Post("/configuration/mappings")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createMapping(HttpServerRequest request) {
		RequestUtils.bodyToJson(request, r->{
			mappingService.create(r).setHandler(res->{
				if(res.succeeded()){
					reloadMapping(request);
				}else{
					Renders.renderError(request, new JsonObject().put("error", "cas.mappings.cantcreate"));
					log.error("Failed to create mapping : ", res.cause());
				}
			});
		});
	}

	public void loadPatterns() {
		mappingService.reset();
		eb.send("wse.app.registry.bus", new JsonObject().put("action", "list-cas-connectors"),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					services.cleanPatterns();
					JsonArray externalApps = event.body().getJsonArray("result");
					for (Object o: externalApps) {
						if (!(o instanceof JsonObject)) continue;
						final JsonObject j = (JsonObject) o;
						final String service = j.getString("service");
						final String structureId = j.getString("structureId");
						final JsonArray patterns = j.getJsonArray("patterns");
						final boolean emptyPattern = j.getBoolean("emptyPattern", false);
						if (service != null && !service.trim().isEmpty() && patterns != null && patterns.size() > 0) {
							services.addPatterns(emptyPattern, service,structureId, Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class));
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
				final String structureId = message.body().getString("structureId");
				final String service = message.body().getString("service");
				final JsonArray patterns = message.body().getJsonArray("patterns");
				final boolean emptyPattern = message.body().getBoolean("emptyPattern", false);
				message.reply(new JsonObject().put("status",
						services.addPatterns(emptyPattern, service, structureId, Arrays.copyOf(patterns.getList().toArray(), patterns.size(), String[].class)) ? "ok" : "error"));
				break;
			default:
				message.reply(new JsonObject().put("status", "error").put("message", "invalid.action"));
		}
	}

	public void setRegisteredServices(RegisteredServices services) {
		this.services = services;
	}

}
