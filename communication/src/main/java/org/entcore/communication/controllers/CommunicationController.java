/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.communication.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.services.impl.DefaultCommunicationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class CommunicationController extends BaseController {

	private final CommunicationService communicationService = new DefaultCommunicationService();

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void adminConsole(final HttpServerRequest request) {
		renderView(request);
	}

	@Post("/group/:startGroupId/communique/:endGroupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addLink(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;
		communicationService.addLink(params.getStartGroupId(), params.getEndGroupId(),
				notEmptyResponseHandler(request));
	}

	@Delete("/group/:startGroupId/communique/:endGroupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeLink(HttpServerRequest request) {
		Params params = new Params(request).validate();
		if (params.isInvalid()) return;
		communicationService.removeLink(params.getStartGroupId(), params.getEndGroupId(),
				notEmptyResponseHandler(request));
	}

	@Post("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addLinksWithUsers(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.addLinkWithUsers(groupId, direction, notEmptyResponseHandler(request));
	}

	@Delete("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeLinksWithUsers(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.removeLinkWithUsers(groupId, direction, notEmptyResponseHandler(request));
	}

	@Get("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void communiqueWith(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		communicationService.communiqueWith(groupId, notEmptyResponseHandler(request));
	}

	@Post("/relative/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addLinkBetweenRelativeAndStudent(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.addLinkBetweenRelativeAndStudent(groupId, direction, notEmptyResponseHandler(request));
	}

	@Delete("/relative/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeLinkBetweenRelativeAndStudent(HttpServerRequest request) {
		String groupId = getGroupId(request);
		if (groupId == null) return;
		CommunicationService.Direction direction = getDirection(request.params().get("direction"));
		communicationService.removeLinkBetweenRelativeAndStudent(groupId, direction, notEmptyResponseHandler(request));
	}

	@Get("/visible/:userId")
	@SecuredAction("communication.visible.user")
	public void visibleUsers(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String schoolId = request.params().get("schoolId");
			List<String> expectedTypes = request.params().getAll("expectedType");
			visibleUsers(userId, schoolId, new JsonArray(expectedTypes.toArray()), arrayResponseHandler(request));
		} else {
			renderJson(request, new JsonArray());
		}
	}

	@BusAddress("wse.communication.users")
	public void visibleUsers(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String action = message.body().getString("action", "");
			String schoolId = message.body().getString("schoolId");
			JsonArray expectedTypes = message.body().getArray("expectedTypes");
			Handler<Either<String, JsonArray>> responseHandler = new Handler<Either<String, JsonArray>>() {

				@Override
				public void handle(Either<String, JsonArray> res) {
					JsonArray j;
					if (res.isRight()) {
						j = res.right().getValue();
					} else {
						log.warn(res.left().getValue());
						j = new JsonArray();
					}
					message.reply(j);
				}
			};
			switch (action) {
			case "visibleUsers":
				String preFilter = message.body().getString("preFilter");
				String customReturn = message.body().getString("customReturn");
				JsonObject ap = message.body().getObject("additionnalParams");
				boolean itSelf = message.body().getBoolean("itself", false);
				boolean myGroup = message.body().getBoolean("mygroup", false);
				boolean profile = message.body().getBoolean("profile", true);
				communicationService.visibleUsers(userId, schoolId, expectedTypes, itSelf, myGroup,
						profile, preFilter, customReturn, ap, responseHandler);
				break;
			case "usersCanSeeMe":
				communicationService.usersCanSeeMe(userId, responseHandler);
				break;
			case "visibleProfilsGroups":
				String pF = message.body().getString("preFilter");
				String c = message.body().getString("customReturn");
				JsonObject p = message.body().getObject("additionnalParams");
				communicationService.visibleProfilsGroups(userId, c, p, pF, responseHandler);
				break;
			case "visibleManualGroups":
				String cr = message.body().getString("customReturn");
				JsonObject pa = message.body().getObject("additionnalParams");
				communicationService.visibleManualGroups(userId, cr, pa, responseHandler);
				break;
			default:
				message.reply(new JsonArray());
				break;
			}
		} else {
			message.reply(new JsonArray());
		}
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes,
			final Handler<Either<String, JsonArray>> handler) {
		visibleUsers(userId, schoolId, expectedTypes, false, null, null, handler);
	}

	private void visibleUsers(String userId, String schoolId, JsonArray expectedTypes, boolean itSelf,
			String customReturn, JsonObject additionnalParams, Handler<Either<String, JsonArray>> handler) {
		communicationService.visibleUsers(
				userId, schoolId, expectedTypes, itSelf, false, true, null, customReturn, additionnalParams, handler);
	}

	@Put("/init/rules")
	@SecuredAction("communication.init.default.rules")
	public void initDefaultCommunicationRules(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				JsonObject initDefaultRules = container.config().getObject("initDefaultCommunicationRules");
				JsonArray structures = body.getArray("structures");
				if (structures != null && structures.size() > 0) {
					communicationService.initDefaultRules(structures,
							initDefaultRules, defaultResponseHandler(request));
				} else {
					badRequest(request, "invalid.structures");
				}
			}
		});
	}

	/**
	 * Send the default communication rules contained inside the mod.json file.
	 * @param request Incoming request.
	 */
	@Get("/rules")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void getDefaultCommunicationRules(final HttpServerRequest request) {
		JsonObject initDefaultRules = container.config().getObject("initDefaultCommunicationRules");
		Renders.renderJson(request, initDefaultRules, 200);
	}

	@Put("/rules/:structureId")
	@SecuredAction("communication.default.rules")
	public void defaultCommunicationRules(final HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		if (structureId == null || structureId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		communicationService.applyDefaultRules(new JsonArray().add(structureId),
				defaultResponseHandler(request));
	}

	@BusAddress("wse.communication")
	public void communicationEventBusHandler(final Message<JsonObject> message) {
		JsonObject initDefaultRules = container.config().getObject("initDefaultCommunicationRules");
		final Handler<Either<String, JsonObject>> responseHandler = new Handler<Either<String, JsonObject>>() {

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
		switch (message.body().getString("action", "")) {
			case "initDefaultCommunicationRules" :
				communicationService.initDefaultRules(message.body().getArray("schoolIds"),
						initDefaultRules, responseHandler);
				break;
			case "initAndApplyDefaultCommunicationRules" :
				communicationService.initDefaultRules(message.body().getArray("schoolIds"),
						initDefaultRules, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							communicationService.applyDefaultRules(
									message.body().getArray("schoolIds"), responseHandler);
						} else {
							message.reply(new JsonObject().putString("status", "error")
									.putString("message", event.left().getValue()));
						}
					}
				});
				break;
			case "setDefaultCommunicationRules" :
				communicationService.applyDefaultRules(new JsonArray().add(
						message.body().getString("schoolId")), responseHandler);
				break;
			case "setMultipleDefaultCommunicationRules" :
				communicationService.applyDefaultRules(
						message.body().getArray("schoolIds"), responseHandler);
				break;
			case "setCommunicationRules" :
				communicationService.applyRules(
						message.body().getString("groupId"), responseHandler);
				break;
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	@Delete("/rules")
	@SecuredAction("communication.remove.rules")
	public void removeCommunicationRules(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		communicationService.removeRules(structureId, defaultResponseHandler(request));
	}

	private class Params {
		private boolean myResult;
		private HttpServerRequest request;
		private String startGroupId;
		private String endGroupId;

		public Params(HttpServerRequest request) {
			this.request = request;
		}

		boolean isInvalid() {
			return myResult;
		}

		public String getStartGroupId() {
			return startGroupId;
		}

		public String getEndGroupId() {
			return endGroupId;
		}

		public Params validate() {
			startGroupId = request.params().get("startGroupId");
			endGroupId = request.params().get("endGroupId");
			if (startGroupId == null || startGroupId.trim().isEmpty() ||
					endGroupId == null || endGroupId.trim().isEmpty()) {
				badRequest(request, "invalid.parameter");
				myResult = true;
				return this;
			}
			myResult = false;
			return this;
		}
	}

	private CommunicationService.Direction getDirection(String direction) {
		CommunicationService.Direction d;
		try {
			d = CommunicationService.Direction.valueOf(direction.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			d = CommunicationService.Direction.BOTH;
		}
		return d;
	}

	private String getGroupId(HttpServerRequest request) {
		String groupId = request.params().get("groupId");
		if (groupId == null || groupId.trim().isEmpty()) {
			badRequest(request, "invalid.parameter");
			return null;
		}
		return groupId;
	}

}
