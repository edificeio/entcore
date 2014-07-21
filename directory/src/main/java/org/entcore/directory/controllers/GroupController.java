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

import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.impl.DefaultGroupService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.platform.Container;

import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class GroupController extends Controller {

	private final GroupService groupService;

	public GroupController(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		groupService = new DefaultGroupService();
	}

	@fr.wseduc.security.SecuredAction(value = "group.list.admin", type = ActionType.RESOURCE)
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
}
