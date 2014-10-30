/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.directory.services.ProfileService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class ProfileController extends BaseController {

	private ProfileService profileService;

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
				profileService.createFunctionGroup(event.getArray("functionsCodes"),
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
