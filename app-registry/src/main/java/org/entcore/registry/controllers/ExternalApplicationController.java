package org.entcore.registry.controllers;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.bus.BusResponseHandler.busArrayHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import fr.wseduc.bus.BusAddress;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.registry.filters.ApplicationFilter;
import org.entcore.registry.filters.SuperAdminFilter;
import org.entcore.registry.services.ExternalApplicationService;
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
				final String casType = body.getString("casType","");
				final String address = body.getString("address", "");
				final boolean updateCas = !casType.trim().isEmpty();
				externalAppService.createExternalApplication(structureId, body, new Handler<Either<String,JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if(event.isRight() && updateCas){
							String pattern = body.getString("pattern", "");
							if(pattern.isEmpty() && !address.isEmpty()){
								try {
									URL addressURL;
									if(address.startsWith("/adapter#")){
										addressURL = new URL(address.substring(address.indexOf("#") + 1));
									} else {
										addressURL = new URL(address);
									}
									pattern = "^\\Q" + addressURL.getProtocol() + "://" + addressURL.getHost() + (addressURL.getPort() > 0 ? ":" + addressURL.getPort() : "") + "\\E.*";
								} catch (MalformedURLException e) {
									pattern = "";
								}
							}
							Server.getEventBus(vertx).publish("cas.configuration", new JsonObject()
								.putString("action", "add-patterns")
								.putString("service", casType)
								.putArray("patterns", new JsonArray().add(pattern)));
						}
						notEmptyResponseHandler(request, 201, 409).handle(event);
					}
				});
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
