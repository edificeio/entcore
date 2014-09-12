/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
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


public class UserController extends BaseController {

	private static final String NOTIFICATION_TYPE = "USERBOOK";
	private UserService userService;
	private UserBookService userBookService;
	private TimelineHelper notification;

	@Put("/user/:userId")
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

	@Put("/userbook/:userId")
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

	@Get("/user/:userId")
	@SecuredAction(value = "user.get", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userService.get(userId, notEmptyResponseHandler(request));
	}

	@Get("/userbook/:userId")
	@SecuredAction(value = "user.get.userbook", type = ActionType.RESOURCE)
	public void getUserBook(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userBookService.get(userId, notEmptyResponseHandler(request));
	}

	@Put("/avatar/:userId")
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

	@Get("/list/isolated")
	@SecuredAction("user.list.isolated")
	public void listIsolated(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final List<String> expectedProfile = request.params().getAll("profile");
		userService.listIsolated(structureId, expectedProfile, arrayResponseHandler(request));
	}

	@Delete("/user")
	@SecuredAction(value = "user.delete", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		List<String> users = request.params().getAll("userId");
		userService.delete(users, defaultResponseHandler(request));
	}

	@Get("/export/users")
	@SecuredAction("user.export")
	public void export(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final String classId = request.params().get("classId");
		JsonArray types = new JsonArray(request.params().getAll("profile").toArray());
		Handler<Either<String, JsonArray>> handler;
		if ("csv".equals(request.params().get("format"))) {
			handler = new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> r) {
					if (r.isRight()) {
						processTemplate(request, "text/export.id.txt",
								new JsonObject().putArray("list", r.right().getValue()), new Handler<String>() {
							@Override
							public void handle(final String export) {
								if (export != null) {
									request.response().putHeader("Content-Type", "application/csv");
									request.response().putHeader("Content-Disposition",
											"attachment; filename=export.csv");
									request.response().end(export);
								} else {
									renderError(request);
								}
							}
						});
					} else {
						renderJson(request, new JsonObject().putString("error", r.left().getValue()), 400);
					}
				}
			};
		} else {
			handler = arrayResponseHandler(request);
		}
		userService.list(structureId, classId, types, handler);
	}

	@Post("/user/function/:userId")
	@SecuredAction("user.add.function")
	public void addFunction(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, pathPrefix + "addFunction", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				userService.addFunction(userId, event.getString("functionCode"),
						event.getArray("structures"), event.getArray("classes"), defaultResponseHandler(request));
			}
		});
	}

	@Delete("/user/function/:userId/:function")
	@SecuredAction("user.remove.function")
	public void removeFunction(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String function = request.params().get("function");
		userService.removeFunction(userId, function, defaultResponseHandler(request));
	}

	@Post("/user/group/:userId/:groupId")
	@SecuredAction("user.add.group")
	public void addGroup(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String groupId = request.params().get("groupId");
		userService.addGroup(userId, groupId, defaultResponseHandler(request));
	}

	@Delete("/user/group/:userId/:groupId")
	@SecuredAction("user.remove.group")
	public void removeGroup(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String groupId = request.params().get("groupId");
		userService.removeGroup(userId, groupId, defaultResponseHandler(request));
	}

	@Get("/user/admin/list")
	@SecuredAction(value = "user.list.admin", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String structureId = request.params().get("structureId");
					final String classId = request.params().get("classId");
					final JsonArray types = new JsonArray(request.params().getAll("profile").toArray());
					final String groupId = request.params().get("groupId");
					userService.listAdmin(structureId, classId, groupId, types, user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public void setUserBookService(UserBookService userBookService) {
		this.userBookService = userBookService;
	}

	public void setNotification(TimelineHelper notification) {
		this.notification = notification;
	}

}
