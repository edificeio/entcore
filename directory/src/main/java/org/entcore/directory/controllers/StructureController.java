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

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.SchoolService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class StructureController extends BaseController {

	private SchoolService structureService;

	@Put("/structure/:structureId/link/:userId")
	@SecuredAction("structure.link.user")
	public void linkUser(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String userId = request.params().get("userId");
		structureService.link(structureId, userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					if (r.right().getValue() != null && r.right().getValue().size() > 0) {
						JsonArray a = new JsonArray().addString(userId);
						ApplicationUtils.sendModifiedUserGroup(eb, a, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								JsonObject j = new JsonObject()
										.putString("action", "setDefaultCommunicationRules")
										.putString("schoolId", structureId);
								eb.send("wse.communication", j);
							}
						});
						renderJson(request, r.right().getValue(), 200);
					} else {
						notFound(request);
					}
				} else {
					renderJson(request, new JsonObject().putString("error", r.left().getValue()), 400);
				}
			}
		});
	}

	@Delete("/structure/:structureId/unlink/:userId")
	@SecuredAction("structure.unlink.user")
	public void unlinkUser(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String structureId = request.params().get("structureId");
		structureService.unlink(structureId, userId, notEmptyResponseHandler(request));
	}

	@Get("/structure/admin/list")
	@SecuredAction(value = "structure.list.admin", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					structureService.listAdmin(user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/structure/:structureId/parent/:parentStructureId")
	@SecuredAction("structure.define.parent")
	public void defineParent(final HttpServerRequest request) {
		final String parentStructureId = request.params().get("parentStructureId");
		final String structureId = request.params().get("structureId");
		structureService.defineParent(structureId, parentStructureId, notEmptyResponseHandler(request));
	}

	public void setStructureService(SchoolService structureService) {
		this.structureService = structureService;
	}
}

