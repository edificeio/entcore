/*
 * Copyright © WebServices pour l'Éducation, 2015
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
