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

package org.entcore.timeline.controllers;

import java.util.Map;
import java.util.ArrayList;

import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.timeline.services.FlashMsgService;
import org.entcore.timeline.services.impl.FlashMsgServiceSqlImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.vertx.java.core.http.RouteMatcher;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class FlashMsgController extends BaseController {

	private FlashMsgService service = new FlashMsgServiceSqlImpl("flashmsg", "messages");
	private TimelineHelper notification;

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		notification = new TimelineHelper(vertx, eb, config);
	}

	/* User part */

	@Get("/flashmsg/listuser")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void listUser(final HttpServerRequest request) {
		final String language = Utils
				.getOrElse(I18n.acceptLanguage(request), "fr", false)
				.split(",")[0].split("-")[0];

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(UserInfos user) {
				if(user == null){
					badRequest(request);
					return;
				}
				service.listForUser(user, language, getHost(request), arrayResponseHandler(request));
			}
		});
	}

	@Put("/flashmsg/:id/markasread")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void markAsRead(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			public void handle(final UserInfos user) {
				if(user == null){
					badRequest(request);
					return;
				}

				service.markAsRead(user, request.params().get("id"), defaultResponseHandler(request));
			}
		});
	}

	/* Admin part */

	@Get("/flashmsg/admin")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void viewAdmin(final HttpServerRequest request){
		renderView(request, new JsonObject(), "admin-flashmsg.html", null);
	}

	@Get("/flashmsg/preview")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void preview(final HttpServerRequest request){
		renderView(request, new JsonObject(), "admin-preview.html", null);
	}

	@Post("/flashmsg")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void create(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				RequestUtils.bodyToJson(request, pathPrefix + "flashmsg.create", new Handler<JsonObject>() {
					public void handle(JsonObject body) {
						if(body == null){
							badRequest(request);
							return;
						}

						body.put("domain", getHost(request));
						body.put("author", user.getUsername());
						body.put("lastModifier", user.getUsername());

						service.create(body, defaultResponseHandler(request));
					}
				});
			}
		});
	}

	@Delete("/flashmsg/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void delete(final HttpServerRequest request) {
		service.delete(request.params().get("id"), defaultResponseHandler(request));
	}
	@Delete("/flashmsg")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void deleteMultiple(final HttpServerRequest request) {
		service.deleteMultiple(request.params().getAll("id"), defaultResponseHandler(request));
	}

	@Put("/flashmsg/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void update(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				RequestUtils.bodyToJson(request, pathPrefix + "flashmsg.update", new Handler<JsonObject>() {
					public void handle(JsonObject body) {
						body.put("lastModifier", user.getUsername());
						service.update(request.params().get("id"), body, defaultResponseHandler(request));
					}
				});
			}
		});
	}

	@Get("/flashmsg/listadmin")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void listAdmin(final HttpServerRequest request) {
		service.list(getHost(request), arrayResponseHandler(request));
	}

	@Get("/flashmsg/listadmin/:structureId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void listAdminByStructureId(final HttpServerRequest request) {
		service.listByStructureId(request.params().get("structureId"), arrayResponseHandler(request));
	}

	@Get("/flashmsg/:messageId/substructures")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void getSubstructuresByMessageId(final HttpServerRequest request) {
		service.getSubstructuresByMessageId(request.params().get("messageId"), arrayResponseHandler(request));
	}

	@Post("/flashmsg/:messageId/substructures")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void setSubstructuresByMessageId(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			public void handle(JsonObject body) {
				service.setSubstructuresByMessageId(request.params().get("messageId"), body, arrayResponseHandler(request));
			}
		});
	}

	@Post("/flashmsg/notify")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void notify(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
					public void handle(JsonObject body) {
						if(body == null){
							badRequest(request);
							return;
						}

						ArrayList<String> recipientIds = new ArrayList<>();
						JsonArray ids = body.getJsonArray("recipientIds");
						for (int i = 0; i < ids.size(); i++) {
							recipientIds.add(ids.getString(i));
						}

						String content = body.getString("content");
						boolean sendMailNotification = body.getBoolean("mailNotification");
						boolean sendPushNotification = body.getBoolean("pushNotification");

						final JsonObject params = new JsonObject()
							.put("username", user.getUsername())
							.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
							.put("content", content);

						if (sendMailNotification && !recipientIds.isEmpty()) {
							notification.notifyTimeline(request, "timeline.send-flash-message-mail", user,
							recipientIds, params);
						}

						if (sendPushNotification && !recipientIds.isEmpty()) {
							// TO DO: Send push notifications
						}

						request.response().setStatusCode(200).end();
					}
				});
			}
		});
	}

}
