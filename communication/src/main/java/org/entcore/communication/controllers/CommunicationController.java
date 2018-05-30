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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.communication.services.CommunicationService;
import org.entcore.communication.services.impl.DefaultCommunicationService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

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
			visibleUsers(userId, schoolId, new fr.wseduc.webutils.collections.JsonArray(expectedTypes), arrayResponseHandler(request));
		} else {
			renderJson(request, new fr.wseduc.webutils.collections.JsonArray());
		}
	}

	@Post("/visible")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void searchVisible(HttpServerRequest request) {
		RequestUtils.bodyToJson(request, filter -> UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				String preFilter = "";
				String match = "";
				String where = "";
				JsonObject params = new JsonObject();
				JsonArray expectedTypes = null;
				if (filter != null && filter.size()> 0) {
					for (String criteria : filter.fieldNames()) {
						switch (criteria) {
							case "structures":
							case "classes":
								if (!params.containsKey("nIds")) {
									params.put("nIds", filter.getJsonArray(criteria));
								} else {
									params.getJsonArray("nIds").addAll(filter.getJsonArray(criteria));
								}
								if (!match.contains("(visibles)-[:IN*0..1]->(g)")) {
									match = "MATCH (visibles)-[:IN*0..1]->(g)-[:DEPENDS]->(n) ";
									where = "WHERE n.id IN {nIds} ";
								} else if (!match.contains("-[:DEPENDS]->(n)")) {
									match += "-[:DEPENDS]->(n) ";
									where += "AND n.id IN {nIds} ";
								}
								break;

//								JsonArray profiles = filter.getJsonArray(criteria);
//								if (profiles != null && profiles.size() > 1) {
//									preFilter.append("AND HEAD(m.profiles) IN {profiles} ");
//									//for ()
//								}
								//break;
							case "profiles":
							case "functions":
								if (!params.containsKey("filters")) {
									params.put("filters", filter.getJsonArray(criteria));
								} else {
									params.getJsonArray("filters").addAll(filter.getJsonArray(criteria));
								}
								if (!match.contains("(visibles)-[:IN*0..1]->(g)")) {
									match = "MATCH (visibles)-[:IN*0..1]->(g)";
									where = " WHERE g.filter IN {filters} ";
								} else if (!where.contains("g.filter IN {filters}")) {
									where += "AND  g.filter IN {filters} ";
								}
								break;
							case "search":
								final String search = filter.getString(criteria);
								if (isNotEmpty(search)) {
									preFilter = "AND m.displayNameSearchField CONTAINS {search} ";
									String sanitizedSearch = StringValidation.removeAccents(search.trim()).toLowerCase();
									params.put("search", sanitizedSearch);
								}
								break;
							case "types":
								JsonArray types = filter.getJsonArray(criteria);
								if (types != null && types.size() > 0 && CommunicationService.EXPECTED_TYPES.containsAll(types.getList())) {
									expectedTypes = types;
								}
								break;
						}
					}
				}
				final String customReturn = match + where +
						"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
						"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
						"HEAD(visibles.profiles) as profile";
				communicationService.visibleUsers(user.getUserId(), null, expectedTypes, false, true, false,
						preFilter, customReturn, params, visibles -> {
							if (visibles.isRight()) {
								renderJson(request,
										UserUtils.translateAndGroupVisible(visibles.right().getValue(),
												I18n.acceptLanguage(request)));
							} else {
								leftToResponse(request, visibles.left());
							}
						});
			} else {
				badRequest(request, "invalid.user");
			}
		}));
	}

	@Get("/visible/group/:groupId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void visibleGroupContains(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				String groupId = request.params().get("groupId");
				if (groupId == null || groupId.trim().isEmpty()) {
					badRequest(request, "invalid.groupId");
					return;
				}
				String customReturn =
						"MATCH (s:Group { id : {groupId}})<-[:IN]-(visibles) " +
						"RETURN DISTINCT HEAD(visibles.profiles) as type, visibles.id as id, " +
						"visibles.displayName as displayName, visibles.login as login " +
						"ORDER BY type DESC, displayName ";
				final JsonObject params = new JsonObject().put("groupId", groupId);
				communicationService.visibleUsers(user.getUserId(), null, null, true, true, false, null,
						customReturn, params, arrayResponseHandler(request));
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	@BusAddress("wse.communication.users")
	public void visibleUsers(final Message<JsonObject> message) {
		String userId = message.body().getString("userId");
		if (userId != null && !userId.trim().isEmpty()) {
			String action = message.body().getString("action", "");
			String schoolId = message.body().getString("schoolId");
			JsonArray expectedTypes = message.body().getJsonArray("expectedTypes");
			Handler<Either<String, JsonArray>> responseHandler = new Handler<Either<String, JsonArray>>() {

				@Override
				public void handle(Either<String, JsonArray> res) {
					JsonArray j;
					if (res.isRight()) {
						j = res.right().getValue();
					} else {
						log.warn(res.left().getValue());
						j = new fr.wseduc.webutils.collections.JsonArray();
					}
					message.reply(j);
				}
			};
			switch (action) {
			case "visibleUsers":
				String preFilter = message.body().getString("preFilter");
				String customReturn = message.body().getString("customReturn");
				JsonObject ap = message.body().getJsonObject("additionnalParams");
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
				JsonObject p = message.body().getJsonObject("additionnalParams");
				communicationService.visibleProfilsGroups(userId, c, p, pF, responseHandler);
				break;
			case "visibleManualGroups":
				String cr = message.body().getString("customReturn");
				JsonObject pa = message.body().getJsonObject("additionnalParams");
				communicationService.visibleManualGroups(userId, cr, pa, responseHandler);
				break;
			default:
				message.reply(new fr.wseduc.webutils.collections.JsonArray());
				break;
			}
		} else {
			message.reply(new fr.wseduc.webutils.collections.JsonArray());
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
				JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
				JsonArray structures = body.getJsonArray("structures");
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
		JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
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
		communicationService.applyDefaultRules(new fr.wseduc.webutils.collections.JsonArray().add(structureId),
				defaultResponseHandler(request));
	}

	@BusAddress("wse.communication")
	public void communicationEventBusHandler(final Message<JsonObject> message) {
		JsonObject initDefaultRules = config.getJsonObject("initDefaultCommunicationRules");
		final Handler<Either<String, JsonObject>> responseHandler = new Handler<Either<String, JsonObject>>() {

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
		switch (message.body().getString("action", "")) {
			case "initDefaultCommunicationRules" :
				communicationService.initDefaultRules(message.body().getJsonArray("schoolIds"),
						initDefaultRules, responseHandler);
				break;
			case "initAndApplyDefaultCommunicationRules" :
				communicationService.initDefaultRules(message.body().getJsonArray("schoolIds"),
						initDefaultRules, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							communicationService.applyDefaultRules(
									message.body().getJsonArray("schoolIds"), responseHandler);
						} else {
							message.reply(new JsonObject().put("status", "error")
									.put("message", event.left().getValue()));
						}
					}
				});
				break;
			case "setDefaultCommunicationRules" :
				communicationService.applyDefaultRules(new fr.wseduc.webutils.collections.JsonArray().add(
						message.body().getString("schoolId")), responseHandler);
				break;
			case "setMultipleDefaultCommunicationRules" :
				communicationService.applyDefaultRules(
						message.body().getJsonArray("schoolIds"), responseHandler);
				break;
			case "setCommunicationRules" :
				communicationService.applyRules(
						message.body().getString("groupId"), responseHandler);
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
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
