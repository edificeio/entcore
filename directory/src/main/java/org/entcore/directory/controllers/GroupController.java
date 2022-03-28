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
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.GroupService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

import java.util.List;

import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

public class GroupController extends BaseController {

	private GroupService groupService;

	@Get("/group/admin/list")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String structureId = request.params().get("structureId");
					final JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("type"));
					final Boolean translate= request.params().contains("translate") ?
							new Boolean(request.params().get("translate")) :
							Boolean.FALSE;

					final Boolean recursive = request.params().contains("recursive") ?
							new Boolean(request.params().get("recursive")) :
							Boolean.FALSE;

					final Boolean onlyAutomaticGroups = request.params().contains("onlyAutomaticGroups") ?
							new Boolean(request.params().get("onlyAutomaticGroups")) :
							Boolean.FALSE;

					final Handler<Either<String, JsonArray>> handler;
					if(translate){
						handler = new Handler<Either<String, JsonArray>>() {
							@Override
							public void handle(Either<String, JsonArray> r) {
								if (r.isRight()) {
									JsonArray res = r.right().getValue();
									UserUtils.translateGroupsNames(res, I18n.acceptLanguage(request));
									renderJson(request, res);
								} else {
									leftToResponse(request, r.left());
								}
							}
						};
					}else{
						handler = arrayResponseHandler(request);
					}
					groupService.listAdmin(structureId, onlyAutomaticGroups, recursive, user, types, handler);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	/**
	 * Retrieve Func and Disciplines Groups, example: "PRINCIPAL ADJOINT", "CPE", "Education Physique et Sportive"
	 * query param:
	 * - structureId: String
	 * - recursive: boolean, true => will search in structureId and substructures (if user is ADML of substructures)
	 * @param request
	 */
	@Get("/group/admin/funcAndDisciplines")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getFuncGroups(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String structureId = request.params().get("structureId");
					final Boolean recursive = request.params().contains("recursive") ?
							new Boolean(request.params().get("recursive")) :
							Boolean.FALSE;
					groupService.getFuncAndDisciplinesGroups(structureId, recursive, user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/group")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void create(final HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createManualGroup", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String structureId = body.getString("structureId");
				final String classId = body.getString("classId");
				body.remove("structureId");
				body.remove("classId");

				UserUtils.getUserInfos(eb, request, user -> {
					if(user != null) {
						if(UserUtils.isSuperAdmin(user) == false) {
							body.remove("autolinkTargetAllStructs");
							body.remove("autolinkTargetStructs");
							body.remove("autolinkUsersFromGroups");
						}

						groupService.createOrUpdateManual(body, structureId, classId, notEmptyResponseHandler(request, 201));	
					}
					else
						unauthorized(request, "invalid.user");
				});
			}
		});
	}

	@Put("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request)
	{
		final String groupId = request.params().get("groupId");
		if (groupId != null && !groupId.trim().isEmpty()) {
			bodyToJson(request, pathPrefix + "updateManualGroup", new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject body) {
					body.put("id", groupId);

					UserUtils.getUserInfos(eb, request, user -> {
						if(user != null) {
							if(UserUtils.isSuperAdmin(user) == false) {
								body.remove("autolinkTargetAllStructs");
								body.remove("autolinkTargetStructs");
								body.remove("autolinkUsersFromGroups");
							}

							groupService.createOrUpdateManual(body, null, null, notEmptyResponseHandler(request));
						}
						else
							unauthorized(request, "invalid.user");
					});
				}
			});
		} else {
			badRequest(request, "invalid.id");
		}
	}

	@Delete("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		if (groupId != null && !groupId.trim().isEmpty()) {
			groupService.deleteManual(groupId, defaultResponseHandler(request, 204));
		} else {
			badRequest(request, "invalid.id");
		}
	}

	@Put("/group/:groupId/users/add")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void addUsers(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		if (groupId != null && !groupId.trim().isEmpty()) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject body) {
					final JsonArray userIds = body.getJsonArray("userIds");
					groupService.addUsers(groupId, userIds, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> res) {
							if (res.isRight()) {
								JsonObject j = new JsonObject()
										.put("action", "setCommunicationRules")
										.put("groupId", groupId);
								eb.send("wse.communication", j);
								ApplicationUtils.publishModifiedUserGroup(eb, userIds);
								renderJson(request, res.right().getValue());
							} else {
								renderJson(request, new JsonObject().put("error", res.left().getValue()), 400);
							}
						}
					});
				}
			});
		}
	}
	
	public void setGroupService(GroupService groupService) {
		this.groupService = groupService;
	}

	@Put("/group/:groupId/users/delete")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void removeUsers(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		if (groupId != null && !groupId.trim().isEmpty()) {
			bodyToJson(request, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject body) {
					final JsonArray userIds = body.getJsonArray("userIds");
					groupService.removeUsers(groupId, userIds, defaultResponseHandler(request));
				}
			});
		}
	}

	@Get("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getGroup(HttpServerRequest request) {
		groupService.getInfos(request.params().get("groupId"), r -> {
			if (r.isRight()) {
				JsonObject res = r.right().getValue();
				UserUtils.translateGroupsNames(new JsonArray().add(res), I18n.acceptLanguage(request));
				renderJson(request, res);
			} else {
				leftToResponse(request, r.left());
			}
		});
	}

}
