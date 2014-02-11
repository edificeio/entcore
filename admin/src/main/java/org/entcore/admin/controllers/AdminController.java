package org.entcore.admin.controllers;

import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.Controller;
import fr.wseduc.security.SecuredAction;

public class AdminController extends Controller {

	public AdminController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
			super(vertx, container, rm, securedActions);
	}

	@SecuredAction("admin.view")
	public void admin(HttpServerRequest request) {
		renderView(request, container.config());
	}

}