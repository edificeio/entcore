package org.entcore.directory.controllers;

import edu.one.core.infra.Controller;
import edu.one.core.infra.NotificationHelper;
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

import static edu.one.core.infra.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;


public class UserController extends Controller {

	private final UserService userService;
	private final UserBookService userBookService;

	public UserController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
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

}
