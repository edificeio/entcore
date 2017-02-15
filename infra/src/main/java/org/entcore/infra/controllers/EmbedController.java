package org.entcore.infra.controllers;

import java.util.Map;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.infra.services.EmbedService;
import org.entcore.infra.services.impl.MongoDbEmbedService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.security.SecuredAction;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class EmbedController extends BaseController {

	private JsonArray defaultEmbedProviders;
	private final static String defaultEmbedLocation =
			"./public/json/embed-providers.json";
	private final EmbedService service = new MongoDbEmbedService("embed");

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		this.refreshDefault();
	}

	private boolean refreshDefault() {
		boolean exists = vertx.fileSystem().existsSync(defaultEmbedLocation);
		if(exists) {
			Buffer buff = vertx.fileSystem().readFileSync(defaultEmbedLocation);
			try {
				this.defaultEmbedProviders = new JsonArray(buff.toString("UTF-8"));
			} catch (Exception e) {
				log.error("Default embedded providers file contains invalid json.");
				log.error(e);
				this.defaultEmbedProviders = new JsonArray();
				return false;
			}
			return true;
		} else {
			log.error("Default embedded providers json file not found.");
			this.defaultEmbedProviders = new JsonArray();
			return false;
		}
	}

	@Get("/embed/admin")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void adminView(final HttpServerRequest request){
		renderView(request, new JsonObject(), "embed-admin.html", null);
	}

	@Post("/embed/refresh/default")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void refreshDefaultEmbed(final HttpServerRequest request){
		boolean success = this.refreshDefault();
		if(success)
			renderJson(request, new JsonObject().putString("status", "ok"));
		else
			renderError(request);

	}

	@Get("/embed/default")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getDefaultEmbed(final HttpServerRequest request){
		renderJson(request, this.defaultEmbedProviders);
	}

	@Get("/embed/custom")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getCustomEmbed(final HttpServerRequest request){
		service.list(arrayResponseHandler(request));
	}

	@Post("/embed/custom")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void createEmbed(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null) {
					badRequest(request);
					return;
				}
				RequestUtils.bodyToJson(request, pathPrefix + "embed", new Handler<JsonObject>() {
					public void handle(JsonObject data) {
						service.create(data, user, defaultResponseHandler(request));
					}
				});
			}
		});
	}

	@Put("/embed/custom/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void updateEmbed(final HttpServerRequest request){
		final String id = request.params().get("id");
		if(id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		RequestUtils.bodyToJson(request, pathPrefix + "embed", new Handler<JsonObject>() {
			public void handle(JsonObject data) {
				service.update(id, data, defaultResponseHandler(request));
			}
		});
	}

	@Delete("/embed/custom/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void deleteEmbed(final HttpServerRequest request){
		final String id = request.params().get("id");
		if(id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		service.delete(id,defaultResponseHandler(request));
	}

}
