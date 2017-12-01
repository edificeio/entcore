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

import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.directory.services.TenantService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class TenantController extends BaseController {

	private TenantService tenantService;

	@Post("/tenant")
	@SecuredAction("tenant.create")
	public void create(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createTenant", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				tenantService.create(event, notEmptyResponseHandler(request, 201));
			}
		});
	}

	public void setTenantService(TenantService tenantService) {
		this.tenantService = tenantService;
	}

}
