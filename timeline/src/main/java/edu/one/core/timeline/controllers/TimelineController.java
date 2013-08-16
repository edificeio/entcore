package edu.one.core.timeline.controllers;

import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

import edu.one.core.infra.Controller;
import edu.one.core.infra.security.SecuredAction;

public class TimelineController extends Controller {

	public TimelineController(Vertx vertx, Container container,
			RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
	}

	public void view(HttpServerRequest request) {
		renderView(request);
	}

}
