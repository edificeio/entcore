package org.entcore.common.controller;

import java.util.Map;

import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.security.SecuredAction;

public class ConfController extends BaseController {

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		get("/conf/public", "getPublicConf");
	}

	public void getPublicConf(final HttpServerRequest request){
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					renderJson(request, container.config().getObject("publicConf", new JsonObject()));
				} else {
					unauthorized(request);
				}
			}
		});
	}
}
