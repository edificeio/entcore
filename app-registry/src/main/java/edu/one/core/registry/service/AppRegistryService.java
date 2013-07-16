package edu.one.core.registry.service;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.AbstractService;
import edu.one.core.security.SecuredAction;

public class AppRegistryService extends AbstractService {

	public AppRegistryService(Vertx vertx, Container container, RouteMatcher rm) {
		super(vertx, container, rm);
	}

	public void testExecute(HttpServerRequest request) {
		request.response().end("Test execute works !!!");
	}

	public void collectApps(Message<JsonObject> message) {
		container.logger().info(message.body().encode());
		message.reply((new JsonObject()).putString("status", "ok"));
	}

}
