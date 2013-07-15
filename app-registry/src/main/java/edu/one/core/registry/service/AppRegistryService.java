package edu.one.core.registry.service;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

import edu.one.core.infra.AbstractService;
import edu.one.core.security.SecuredAction;

public class AppRegistryService extends AbstractService {

	public AppRegistryService(Vertx vertx, Container container, RouteMatcher rm) {
		super(vertx, container, rm);
	}

	@SecuredAction("test.action")
	public void testExecute(HttpServerRequest request) {
		request.response().end("Test execute works !!!");
	}

}
