package org.entcore.timeline.controllers;

import java.util.Map;

import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.timeline.services.FlashMsgService;
import org.entcore.timeline.services.impl.FlashMsgServiceSqlImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
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

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
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
	@ResourceFilter(SuperAdminFilter.class)
	public void create(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "flashmsg.create", new Handler<JsonObject>() {
			public void handle(JsonObject body) {
				if(body == null){
					badRequest(request);
					return;
				}

				body.put("domain", getHost(request));

				service.create(body, defaultResponseHandler(request));
			}
		});
	}

	@Delete("/flashmsg/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void delete(final HttpServerRequest request) {
		service.delete(request.params().get("id"), defaultResponseHandler(request));
	}
	@Delete("/flashmsg")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void deleteMultiple(final HttpServerRequest request) {
		service.deleteMultiple(request.params().getAll("id"), defaultResponseHandler(request));
	}

	@Put("/flashmsg/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void update(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "flashmsg.update", new Handler<JsonObject>() {
			public void handle(JsonObject body) {
				service.update(request.params().get("id"), body, defaultResponseHandler(request));
			}
		});
	}

	@Get("/flashmsg/listadmin")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void listAdmin(final HttpServerRequest request) {
		service.list(getHost(request), arrayResponseHandler(request));
	}

}
