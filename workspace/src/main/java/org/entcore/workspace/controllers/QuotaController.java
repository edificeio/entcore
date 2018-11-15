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

package org.entcore.workspace.controllers;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import org.entcore.common.folders.QuotaService;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

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
