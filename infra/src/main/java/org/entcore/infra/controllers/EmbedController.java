/*
 * Copyright © "Open Digital Education", 2018
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

package org.entcore.infra.controllers;

import java.util.Map;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.infra.services.EmbedService;
import org.entcore.infra.services.impl.MongoDbEmbedService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.security.SecuredAction;
import org.vertx.java.core.http.RouteMatcher;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class EmbedController extends BaseController {

	private JsonArray defaultEmbedProviders;
	private final static String defaultEmbedLocation =
			"./public/json/embed-providers.json";
	private final EmbedService service = new MongoDbEmbedService("embed");

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		this.refreshDefault();
	}

	private boolean refreshDefault() {
		boolean exists = vertx.fileSystem().existsBlocking(defaultEmbedLocation);
		if(exists) {
			Buffer buff = vertx.fileSystem().readFileBlocking(defaultEmbedLocation);
			try {
				this.defaultEmbedProviders = new fr.wseduc.webutils.collections.JsonArray(buff.toString("UTF-8"));
			} catch (Exception e) {
				log.error("Default embedded providers file contains invalid json.");
				log.error(e);
				this.defaultEmbedProviders = new fr.wseduc.webutils.collections.JsonArray();
				return false;
			}
			return true;
		} else {
			log.error("Default embedded providers json file not found.");
			this.defaultEmbedProviders = new fr.wseduc.webutils.collections.JsonArray();
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
			renderJson(request, new JsonObject().put("status", "ok"));
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
