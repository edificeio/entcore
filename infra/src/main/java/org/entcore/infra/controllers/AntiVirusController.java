/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.infra.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.infra.services.AntivirusService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AntiVirusController extends BaseController {

	private AntivirusService antivirusService;

	@Post("/antivirus/check")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void checkScanReport(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (event != null && isNotEmpty(event.getString("scanReport"))) {
					antivirusService.replaceInfectedFiles(event.getString("scanReport"), defaultResponseHandler(request));
				}
			}
		});
	}

	@Post("/antivirus/scan")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void scan(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (event != null && isNotEmpty(event.getString("file"))) {
					antivirusService.scan(event.getString("file"));
					ok(request);
				} else {
					badRequest(request);
				}
			}
		});
	}

	public void setAntivirusService(AntivirusService antivirusService) {
		this.antivirusService = antivirusService;
	}

}
