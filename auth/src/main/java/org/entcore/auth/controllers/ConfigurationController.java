/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.auth.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.auth.services.ConfigurationService;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ConfigurationController extends BaseController {

	private ConfigurationService configurationService;

	@Put("/configure/welcome")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void editWelcomeMessage(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				if (body != null) {
					configurationService.editWelcomeMessage(getHost(request), body, defaultResponseHandler(request));
				} else {
					badRequest(request, "invalid.body");
				}
			}
		});
	}

	@Get("/configure/welcome")
	public void getWelcomeMessage(final HttpServerRequest request) {
		final String host = getHost(request);
		final String language = request.params().get("allLanguages") != null ? null : I18n.acceptLanguage(request).split(",")[0].split("\\-")[0];
		configurationService.getWelcomeMessage(host, language, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					if (event.right().getValue() != null && event.right().getValue().size() > 0) {
						JsonObject res = event.right().getValue();
						if (res.getJsonObject(host) != null && res.getJsonObject(host).getString(language) != null) {
							renderJson(request, new JsonObject()
									.put("welcomeMessage", res.getJsonObject(host).getString(language))
									.put("enabled", res.getJsonObject(host).getBoolean("enabled", false))
							);
						} else if (res.getJsonObject(host) != null) {
							if (!res.getJsonObject(host).containsKey("enabled")) {
								res.getJsonObject(host).put("enabled", false);
							}
							renderJson(request, res.getJsonObject(host));
						} else {
							renderJson(request, res);
						}
					} else {
						notFound(request);
					}
				} else {
					badRequest(request, event.left().getValue());
				}
			}
		});
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

}
