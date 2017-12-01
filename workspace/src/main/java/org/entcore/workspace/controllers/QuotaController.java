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

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;

import org.entcore.workspace.service.QuotaService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class QuotaController extends BaseController {

	private QuotaService quotaService;

	@Get("/quota/user/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuota(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		quotaService.quotaAndUsage(userId, notEmptyResponseHandler(request));
	}

	@Get("/quota/structure/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuotaStructure(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		quotaService.quotaAndUsageStructure(structureId, notEmptyResponseHandler(request));
	}

	@Get("/quota/global")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getQuotaGlobal(final HttpServerRequest request) {
		quotaService.quotaAndUsageGlobal(notEmptyResponseHandler(request));
	}

	@Put("/quota")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "updateQuota", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				quotaService.update(object.getJsonArray("users"), object.getLong("quota"), arrayResponseHandler(request));
			}
		});
	}

	@Put("/quota/default/:profile")
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

	@Get("/quota/default")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getDefault(final HttpServerRequest request) {
		quotaService.getDefaultMaxQuota(arrayResponseHandler(request));
	}

	@BusAddress("activation.ack")
	public void initQuota(final Message<JsonObject> message){
		String userId = message.body().getString("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			quotaService.init(userId);
		}
	}

	@BusAddress("org.entcore.workspace.quota")
	public void quotaEventBusHandler(final Message<JsonObject> message){
		Handler<Either<String, JsonObject>> responseHandler = new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> res) {
				if (res.isRight()) {
					message.reply(res.right().getValue());
				} else {
					message.reply(new JsonObject().put("status", "error")
							.put("message", res.left().getValue()));
				}
			}
		};

		String userId = message.body().getString("userId");

		switch (message.body().getString("action", "")) {
			case "getUserQuota" :
				quotaService.quotaAndUsage(userId, responseHandler);
				break;
			case "updateUserQuota" :
				long size = message.body().getLong("size");
				int threshold = message.body().getInteger("threshold");
				quotaService.incrementStorage(userId, size, threshold, responseHandler);
				break;
			default:
				message.reply(new JsonObject().put("status", "error").put("message", "invalid.action"));
		}
	}


	public void setQuotaService(QuotaService quotaService) {
		this.quotaService = quotaService;
	}
}
