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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.directory.services.ProfileService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class ProfileController extends BaseController {

	private ProfileService profileService;

	@Get("/profiles")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void listProfiles(final HttpServerRequest request) {
		profileService.listProfiles(arrayResponseHandler(request));
	}

	@Put("/profiles")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void blockProfiles(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				if (body != null) {
					profileService.blockProfiles(body, defaultResponseHandler(request));
				} else {
					badRequest(request, "invalid.body");
				}
			}
		});
	}

	@Get("/functions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listFunctions(final HttpServerRequest request) {
		profileService.listFunctions(arrayResponseHandler(request));
	}

	@Post("/function/:profile")
	@SecuredAction("profile.create.function")
	public void createFunction(final HttpServerRequest request) {
		final String profile = request.params().get("profile");
		bodyToJson(request, pathPrefix + "createFunction", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				profileService.createFunction(profile, event, notEmptyResponseHandler(request, 201, 409));
			}
		});
	}

	@Delete("/function/:function")
	@SecuredAction("profile.delete.function")
	public void deleteFunction(final HttpServerRequest request) {
		final String function = request.params().get("function");
		profileService.deleteFunction(function, defaultResponseHandler(request, 204));
	}

	@Post("/functiongroup")
	@SecuredAction("profile.create.function.group")
	public void createFunctionGroup(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createFunctionGroup", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				profileService.createFunctionGroup(event.getJsonArray("functionsCodes"),
						event.getString("name"), event.getString("externalId"), notEmptyResponseHandler(request, 201));
			}
		});
	}

	@Delete("/functiongroup/:groupId")
	@SecuredAction("profile.delete.function.group")
	public void deleteFunctionGroup(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		profileService.deleteFunctionGroup(groupId, defaultResponseHandler(request, 204));
	}

	public void setProfileService(ProfileService profileService) {
		this.profileService = profileService;
	}

}
