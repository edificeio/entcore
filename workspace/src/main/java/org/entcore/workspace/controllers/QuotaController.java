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
import fr.wseduc.webutils.security.XSSUtils;
import org.entcore.workspace.service.QuotaService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
				quotaService.update(object.getArray("users"), object.getLong("quota"), arrayResponseHandler(request));
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
					message.reply(new JsonObject().putString("status", "error")
							.putString("message", res.left().getValue()));
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
				message.reply(new JsonObject().putString("status", "error").putString("message", "invalid.action"));
		}
	}

	@Get("/structure/admin/quota/getUsersQuotaActivity")
	public void getUsersQuotaActivity( final HttpServerRequest request ) {
		int quotaFilterNbusers = Integer.parseInt(request.params().get("quotaFilterNbusers"));
		Float quotaFilterPercentageLimit = Float.parseFloat(request.params().get("quotaFilterPercentageLimit"));

		String	quotaFilterSortBy = request.params().get("quotaFilterSortBy");
		String quotaFilterOrderBy = request.params().get("quotaFilterOrderBy");
		String quotaFilterProfile = request.params().get("quotaFilterProfile");
		String structureid = request.params().get("structureid");
		quotaService.listUsersQuotaActivity(structureid, quotaFilterNbusers, quotaFilterSortBy, quotaFilterOrderBy, quotaFilterProfile, quotaFilterPercentageLimit, arrayResponseHandler(request));
	}

	@Put("/structure/admin/quota/saveProfile")
	public void saveStructureQuotaProfile(final HttpServerRequest request)
	{
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					// getting the jsonArray
					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					JsonArray jsonStructure = new JsonArray(obj);
					for (int i = 0; i < jsonStructure.size(); i++) {
						final JsonObject jsonProfile = jsonStructure.get(i);
						quotaService.updateQuotaForProfile(jsonProfile, new Handler<Either<String,JsonObject>>() {
							public void handle(Either<String, JsonObject> result) {
								if (result.isLeft()) {
									log.error("Error saving profile quota.");
								} else {
									// updating quotas for userBooks.
									quotaService.updateQuotaUserBooks(jsonProfile, new Handler<Either<String,JsonObject>>() {
										public void handle(Either<String, JsonObject> result) {
											if (result.isLeft()) {

											}
										}
									});
								}
							}
						});
					}
				} catch (Exception e) {
					log.error("Failed to save Profile Quota");
				}
			}
		});
	}


	@Put("/structure/admin/quota/saveStructure")
	public void saveStructureQuotaStructure(final HttpServerRequest request)
	{
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					// getting the jsonArray
					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					JsonObject jsonStructure = new JsonObject(obj);
					quotaService.updateQuotaForStructure(jsonStructure, new Handler<Either<String,JsonObject>>() {
						public void handle(Either<String, JsonObject> result) {
							if (result.isLeft()) {
								log.error("Error saving structure quota.");
							}
						}
					});
				} catch (Exception e) {
					log.error("Failed to save Structure Quota");
				}
			}
		});
	}

	@Put("/structure/admin/quota/saveActivity")
	public void saveStructureQuotaActivity(final HttpServerRequest request)
	{
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					// getting the jsonArray
					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					JsonArray jsonStructure = new JsonArray(obj);
					for (int i = 0; i < jsonStructure.size(); i++) {
						JsonObject jsonUser = jsonStructure.get(i);
						quotaService.updateQuotaForUser(jsonUser, new Handler<Either<String,JsonObject>>() {
							public void handle(Either<String, JsonObject> result) {
								if (result.isLeft()) {
									log.error("Error saving profile quota.");
								}
							}
						});
					}
				} catch (Exception e) {
					log.error("Failed to save Structure Quota");
				}
			}
		});
	}


	public void setQuotaService(QuotaService quotaService) {
		this.quotaService = quotaService;
	}
}
