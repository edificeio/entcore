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
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.GroupService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

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
					final JsonArray types = new JsonArray(request.params().getAll("type").toArray());
					groupService.listAdmin(structureId, user, types, arrayResponseHandler(request));
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
				body.removeField("structureId");
				body.removeField("classId");
				groupService.createOrUpdateManual(body, structureId, classId, notEmptyResponseHandler(request, 201));
			}
		});
	}

	@Put("/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		if (groupId != null && !groupId.trim().isEmpty()) {
			bodyToJson(request, pathPrefix + "updateManualGroup", new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject body) {
					body.putString("id", groupId);
					groupService.createOrUpdateManual(body, null, null, notEmptyResponseHandler(request));
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
					final JsonArray userIds = body.getArray("userIds");
					groupService.addUsers(groupId, userIds, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> res) {
							if (res.isRight()) {
								JsonObject j = new JsonObject()
										.putString("action", "setCommunicationRules")
										.putString("groupId", groupId);
								eb.send("wse.communication", j);
								ApplicationUtils.publishModifiedUserGroup(eb, userIds);
								renderJson(request, res.right().getValue());
							} else {
								renderJson(request, new JsonObject().putString("error", res.left().getValue()), 400);
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
					final JsonArray userIds = body.getArray("userIds");
					groupService.removeUsers(groupId, userIds, defaultResponseHandler(request));
				}
			});
		}
	}
}
