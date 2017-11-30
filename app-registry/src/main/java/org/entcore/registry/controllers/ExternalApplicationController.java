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

package org.entcore.registry.controllers;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.bus.BusResponseHandler.busArrayHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

import java.net.URL;
import java.util.List;

import fr.wseduc.bus.BusAddress;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.filters.ApplicationFilter;
import org.entcore.registry.filters.SuperAdminFilter;
import org.entcore.registry.services.ExternalApplicationService;
import org.entcore.registry.services.impl.DefaultAppRegistryService;
import org.entcore.registry.services.impl.DefaultExternalApplicationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.http.BaseController;

public class ExternalApplicationController extends BaseController {
	private final ExternalApplicationService externalAppService = new DefaultExternalApplicationService();

	@Get("/external-applications")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listExternalApplications(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		externalAppService.listExternalApps(structureId, arrayResponseHandler(request));
	}

	@Get("/application/external/:id/groups/roles")
	@SecuredAction(type = ActionType.RESOURCE, value = "")
	public void listExternalApplicationRolesWithGroups(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		String connectorId = request.params().get("id");
		externalAppService.listExternalApplicationRolesWithGroups(structureId, connectorId, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> r) {
				if (r.isRight()) {
					JsonArray list = r.right().getValue();
					for (Object res : list) {
						UserUtils.translateGroupsNames(((JsonObject) res).getArray("groups"), I18n.acceptLanguage(request));
					}
					renderJson(request, list);
				} else {
					leftToResponse(request, r.left());
				}
			}
		});
	}

	@Delete("/application/external/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void deleteExternalApplication(final HttpServerRequest request) {
		String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			externalAppService.deleteExternalApplication(id, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.application.id");
		}
	}

	@Post("/application/external")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createExternalApp(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createApplication", new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				String structureId = request.params().get("structureId");
				final String casType = body.getString("casType", "");
				final String address = body.getString("address", "");
				final boolean updateCas = !StringUtils.isEmpty(casType);
				final URL addressURL = DefaultAppRegistryService.checkCasUrl(address);

				//for oauth url is not used
				if (!updateCas || addressURL != null) {
					externalAppService.createExternalApplication(structureId, body, new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> event) {
							if (event.isLeft()) {
								JsonObject error = new JsonObject()
										.putString("error", event.left().getValue());
								Renders.renderJson(request, error, 400);
								return;
							}

							if (event.right().getValue() != null && event.right().getValue().size() > 0) {
								if (updateCas) {
									String pattern = body.getString("pattern", "");
									if (pattern.isEmpty()) {
										pattern = "^\\Q" + addressURL.getProtocol() + "://" + addressURL.getHost() + (addressURL.getPort() > 0 ? ":" + addressURL.getPort() : "") + "\\E.*";
									}
									Server.getEventBus(vertx).publish("cas.configuration", new JsonObject()
											.putString("action", "add-patterns")
											.putString("service", casType)
											.putArray("patterns", new JsonArray().add(pattern)));
								}
								Renders.renderJson(request, event.right().getValue(), 201);
							} else {
								JsonObject error = new JsonObject()
										.putString("error", "appregistry.failed.app");
								Renders.renderJson(request, error, 400);
							}
						}
					});
				} else {
					badRequest(request, "appregistry.failed.app.url");
				}
			}
		});
	}

	@Put("/application/external/:id/lock")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void lockExternalApp(final HttpServerRequest request) {
		String structureId = request.params().get("id");
		externalAppService.toggleLock(structureId, defaultResponseHandler(request));
	}

	@Put("/application/external/:id/authorize")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void authorizeProfiles(final HttpServerRequest request) {
		String applicationId = request.params().get("id");
		List<String> profiles = request.params().getAll("profile");

		if(profiles.isEmpty() || applicationId == null || applicationId.trim().isEmpty()){
			badRequest(request);
			return;
		}

		externalAppService.massAuthorize(applicationId, profiles, defaultResponseHandler(request));
	}

	@Delete("/application/external/:id/authorize")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(ApplicationFilter.class)
	public void unauthorizeProfiles(final HttpServerRequest request) {
		String applicationId = request.params().get("id");
		List<String> profiles = request.params().getAll("profile");

		if(profiles.isEmpty() || applicationId == null || applicationId.trim().isEmpty()){
			badRequest(request);
			return;
		}

		externalAppService.massUnauthorize(applicationId, profiles, defaultResponseHandler(request, 204));
	}

	@BusAddress("external-application")
	public void externalApplications(Message<JsonObject> message) {
		final String structureId = message.body().getString("structureId");
		switch (message.body().getString("action", "")) {
			case "list" :
				externalAppService.listExternalApps(structureId, busArrayHandler(message));
				break;
			default:
				message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.action"));
		}
	}

}
