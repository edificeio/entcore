package edu.one.core.blog.controllers;

import edu.one.core.infra.Controller;
import java.util.Map;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class BlogController extends Controller {

	private JsonObject config;

	public BlogController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.config = config;
		}

	public void blog(HttpServerRequest request) {
		renderView(request);
	}
}
