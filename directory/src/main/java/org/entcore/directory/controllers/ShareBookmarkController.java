/*
 * Copyright © "Open Digital Education", 2018
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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
							JsonArray members = res.getJsonArray("members");
							if (members == null || members.isEmpty()) {
								shareBookmarkService.delete(user.getUserId(), id, dres -> {
									if (dres.isLeft()) {
										log.error("Error deleting sharebookmark " + id + " : " + dres.left().getValue());
									}
								});
								notFound(request, "empty.sharebookmark");
								return;
							}
							res.mergeIn(UserUtils.translateAndGroupVisible(
									members, I18n.acceptLanguage(request), true)
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
