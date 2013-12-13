package edu.one.core.conversation.controllers;

import edu.one.core.infra.Controller;
import edu.one.core.infra.security.SecuredAction;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

import java.util.Map;

public class ConversationController extends Controller {

	public ConversationController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
	}

	@edu.one.core.security.SecuredAction("conversation.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}

}
