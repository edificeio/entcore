/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.directory.controllers;

import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.services.UserBookService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultUserBookService;
import org.entcore.directory.services.impl.DefaultUserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.SessionAttributes.*;


public class UserController extends Controller {

	private static final String NOTIFICATION_TYPE = "USERBOOK";
	private final UserService userService;
	private final UserBookService userBookService;
	private final TimelineHelper notification;
	private final WorkspaceHelper workspaceHelper;

	public UserController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		Neo neo = new Neo(eb,log);
		NotificationHelper notification = new NotificationHelper(vertx, eb, container);
		this.userService = new DefaultUserService(neo, notification, eb);
		this.userBookService = new DefaultUserBookService(neo);
		this.notification = new TimelineHelper(vertx, eb, container);
		String gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		this.workspaceHelper = new WorkspaceHelper(gridfsAddress, eb);
	}

	@SecuredAction(value = "user.update", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String userId = request.params().get("userId");
				userService.update(userId, body, notEmptyResponseHandler(request));
				UserUtils.removeSessionAttribute(eb, userId, PERSON_ATTRIBUTE, null);
			}
		});
	}

	@SecuredAction(value = "user.update.userbook", type = ActionType.RESOURCE)
	public void updateUserBook(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				final String userId = request.params().get("userId");
				userBookService.update(userId, body, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
								@Override
								public void handle(UserInfos user) {
									if (user != null && userId != null && userId.equals(user.getUserId())) {
										notifyTimeline(request, user, body);
									}
								}
							});
							UserUtils.removeSessionAttribute(eb, userId, PERSON_ATTRIBUTE, null);
							renderJson(request, event.right().getValue());
						} else {
							JsonObject error = new JsonObject()
									.putString("error", event.left().getValue());
							renderJson(request, error, 400);
						}
					}
				});
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

	@SecuredAction(value = "user.update.avatar", type = ActionType.RESOURCE)
	public void updateAvatar(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String p = body.getString("picture");
				if (!StringValidation.isAbsoluteDocumentUri(p)) {
					badRequest(request);
					return;
				}
				final JsonObject j = new JsonObject().putString("picture", p);
				userBookService.update(userId, j, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> u) {
						if (u.isRight()) {
							renderJson(request, j);
						} else {
							leftToResponse(request, u.left());
						}
					}
				});
			}
		});
	}

	private void notifyTimeline(final HttpServerRequest request, final UserInfos user, final JsonObject body) {
		if (body == null) {
			return;
		}
		UserUtils.findUsersCanSeeMe(eb, request, new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray users) {
				String mood = body.getString("mood");
				String motto = body.getString("motto");
				List<String> userIds = new ArrayList<>();
				for (Object o : users) {
					JsonObject u = (JsonObject) o;
					userIds.add(u.getString("id"));
				}
				JsonObject params = new JsonObject()
						.putString("uri", container.config().getString("host") + pathPrefix +
								"/annuaire#" + user.getUserId() + "#" + user.getType())
						.putString("username", user.getUsername())
						.putString("motto", motto)
						.putString("moodImg", mood);
				if (mood != null && !mood.trim().isEmpty()) {
					notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
						NOTIFICATION_TYPE + "_MOOD", userIds,
						user.getUserId() + System.currentTimeMillis() + "mood",
						"notify-mood.html", params);
				}
				if (motto != null && !motto.trim().isEmpty()) {
					notification.notifyTimeline(request, user, NOTIFICATION_TYPE,
							NOTIFICATION_TYPE + "_MOTTO", userIds,
							user.getUserId() + System.currentTimeMillis() + "motto",
							"notify-motto.html", params);
				}
			}
		});
	}

	@SecuredAction("user.list.isolated")
	public void listIsolated(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final List<String> expectedProfile = request.params().getAll("profile");
		userService.listIsolated(structureId, expectedProfile, arrayResponseHandler(request));
	}

	@SecuredAction("user.delete")
	public void delete(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userService.delete(userId, defaultResponseHandler(request));
	}

}
