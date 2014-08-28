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

package org.entcore.workspace.controllers;

import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.workspace.service.QuotaService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class QuotaController extends Controller {

	private QuotaService quotaService;

	public QuotaController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
	}

	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuota(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		quotaService.quotaAndUsage(userId, notEmptyResponseHandler(request));
	}

	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuotaStructure(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		quotaService.quotaAndUsageStructure(structureId, notEmptyResponseHandler(request));
	}

	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuotaGlobal(final HttpServerRequest request) {
		quotaService.quotaAndUsageGlobal(notEmptyResponseHandler(request));
	}

	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "updateQuota", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				quotaService.update(object.getArray("users"), object.getLong("quota"), arrayResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void updateDefault(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "updateDefaultQuota", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				String profile = request.params().get("profile");
				quotaService.updateQuotaDefaultMax(profile, object.getLong("defaultQuota"), object.getLong("maxQuota"),
						notEmptyResponseHandler(request));
			}
		});
	}

	public void initQuota(final Message<JsonObject> message){
		String userId = message.body().getString("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			quotaService.init(userId);
		}
	}

	public void setQuotaService(QuotaService quotaService) {
		this.quotaService = quotaService;
	}
}
