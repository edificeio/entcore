package org.entcore.directory.controllers;

import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.services.UserBookService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultUserBookService;
import org.entcore.directory.services.impl.DefaultUserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;


public class UserController extends Controller {

	private final UserService userService;
	private final UserBookService userBookService;

	public UserController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		Neo neo = new Neo(eb,log);
		NotificationHelper notification = new NotificationHelper(vertx, eb, container);
		this.userService = new DefaultUserService(neo, notification);
		this.userBookService = new DefaultUserBookService(neo);
	}

	@SecuredAction(value = "user.update", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String userId = request.params().get("userId");
				userService.update(userId, body, notEmptyResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "user.update.userbook", type = ActionType.RESOURCE)
	public void updateUserBook(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String userId = request.params().get("userId");
				userBookService.update(userId, body, defaultResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "user.get", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userService.get(userId, notEmptyResponseHandler(request));
	}

	@SecuredAction(value = "user.get.userbook", type = ActionType.RESOURCE)
	public void getUserBook(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userBookService.get(userId, notEmptyResponseHandler(request));
	}

}
