/*
 * Copyright © WebServices pour l'Éducation, 2018
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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.ShareBookmarkService;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class ShareBookmarkController extends BaseController {


	private ShareBookmarkService shareBookmarkService;

	@Post("/sharebookmark")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void create(HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createShareBookmark", event -> UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				shareBookmarkService.create(user.getUserId(), event, notEmptyResponseHandler(request, 201));
			} else {
				badRequest(request, "invalid.user");
			}
		}));
	}

	@Put("/sharebookmark/:id")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void update(HttpServerRequest request) {
		bodyToJson(request, pathPrefix + "createShareBookmark", event -> UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				shareBookmarkService.update(user.getUserId(), request.params().get("id"), event, defaultResponseHandler(request));
			} else {
				badRequest(request, "invalid.user");
			}
		}));
	}

	@Get("/sharebookmark/:id")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void get(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				final String id =  request.params().get("id");
				if ("all".equals(id)) {
					shareBookmarkService.list(user.getUserId(), arrayResponseHandler(request));
				} else {
					shareBookmarkService.get(user.getUserId(), id, r -> {
						if (r.isRight()) {
							final JsonObject res = r.right().getValue();
							res.mergeIn(UserUtils.translateAndGroupVisible(
									res.getJsonArray("members"), I18n.acceptLanguage(request), true)
							);
							res.remove("members");
							renderJson(request, res);
						} else {
							leftToResponse(request, r.left());
						}
					});
				}
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	@Delete("/sharebookmark/:id")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void delete(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				shareBookmarkService.delete(user.getUserId(), request.params().get("id"), defaultResponseHandler(request));
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	@Get("/allowSharebookmarks")
	@SecuredAction("directory.allow.sharebookmarks")
	public void allowSharebookmarks(final HttpServerRequest request) {
		// This route is used to create directory.allow.sharebookmarks Workflow right, nothing to do
		request.response().end();
	}

	public void setShareBookmarkService(ShareBookmarkService shareBookmarkService) {
		this.shareBookmarkService = shareBookmarkService;
	}

}
