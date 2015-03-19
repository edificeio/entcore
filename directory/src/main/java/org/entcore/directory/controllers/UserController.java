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
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.security.AdmlOfStructures;
import org.entcore.directory.security.AdmlOfTwoUsers;
import org.entcore.directory.security.RelativeStudentFilter;
import org.entcore.directory.services.UserBookService;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.SessionAttributes.PERSON_ATTRIBUTE;


public class UserController extends BaseController {

	private static final String NOTIFICATION_TYPE = "USERBOOK";
	private UserService userService;
	private UserBookService userBookService;
	private TimelineHelper notification;

	@Put("/user/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userService.get(userId, notEmptyResponseHandler(request));
	}

	@Get("/userbook/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getUserBook(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userBookService.get(userId, notEmptyResponseHandler(request));
	}

	@Put("/avatar/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
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
						.putString("uri", pathPrefix + "/annuaire#" + user.getUserId() + "#" + user.getType())
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
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listIsolated(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		final List<String> expectedProfile = request.params().getAll("profile");
		userService.listIsolated(structureId, expectedProfile, arrayResponseHandler(request));
	}

	@Delete("/user")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void delete(final HttpServerRequest request) {
		List<String> users = request.params().getAll("userId");
		userService.delete(users, defaultResponseHandler(request));
	}

	@Put("/restore/user")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void restore(final HttpServerRequest request) {
		List<String> users = request.params().getAll("userId");
		userService.restore(users, defaultResponseHandler(request));
	}

	@Get("/export/users")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void export(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
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
					userService.listAdmin(structureId, classId, null, types, user, handler);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/user/function/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addFunction(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, pathPrefix + "addFunction", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				userService.addFunction(userId, event.getString("functionCode"),
						event.getArray("scope"), event.getString("inherit", ""), defaultResponseHandler(request));
			}
		});
	}

	@Delete("/user/function/:userId/:function")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeFunction(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String function = request.params().get("function");
		userService.removeFunction(userId, function, defaultResponseHandler(request));
	}

	@Post("/user/group/:userId/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addGroup(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String groupId = request.params().get("groupId");
		userService.addGroup(userId, groupId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> res) {
				if (res.isRight()) {
					JsonObject j = new JsonObject()
							.putString("action", "setCommunicationRules")
							.putString("groupId", groupId);
					eb.send("wse.communication", j);
					JsonArray a = new JsonArray().addString(userId);
					ApplicationUtils.publishModifiedUserGroup(eb, a);
					renderJson(request, res.right().getValue());
				} else {
					renderJson(request, new JsonObject().putString("error", res.left().getValue()), 400);
				}
			}
		});
	}

	@Delete("/user/group/:userId/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void removeGroup(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String groupId = request.params().get("groupId");
		userService.removeGroup(userId, groupId, defaultResponseHandler(request));
	}

	@Get("/user/group/:groupId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listGroup(final HttpServerRequest request) {
		final String groupId = request.params().get("groupId");
		userService.list(groupId, true, null, arrayResponseHandler(request));
	}

	@Get("/user/adml/list/:structureId")
	@SecuredAction("user.adml.list")
	public void listAdml(final HttpServerRequest request) {
		final String structureId = request.params().get("structureId");
		userService.listAdml(structureId, arrayResponseHandler(request));
	}

	@Get("/user/admin/list")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String structureId = request.params().get("structureId");
					final String classId = request.params().get("classId");
					final JsonArray types = new JsonArray(request.params().getAll("profile").toArray());
					final String groupId = request.params().get("groupId");
					final String nameFilter = request.params().get("name");
					userService.listAdmin(structureId, classId, groupId, types, nameFilter, user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/user/:studentId/related/:relativeId")
	@ResourceFilter(RelativeStudentFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void relativeStudent(final HttpServerRequest request) {
		final String studentId = request.params().get("studentId");
		final String relativeId = request.params().get("relativeId");
		userService.relativeStudent(relativeId, studentId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> res) {
				if (res.isRight()) {
					JsonArray structures = res.right().getValue().getArray("structures");
					JsonObject j = new JsonObject()
							.putString("action", "setMultipleDefaultCommunicationRules")
							.putArray("schoolIds", structures);
					eb.send("wse.communication", j);
					JsonArray a = new JsonArray().addString(relativeId);
					ApplicationUtils.publishModifiedUserGroup(eb, a);
					if (structures == null || structures.size() == 0) {
						notFound(request, "user.not.found");
					} else {
						ok(request);
					}
				} else {
					renderJson(request, new JsonObject().putString("error", res.left().getValue()), 400);
				}
			}
		});
	}

	@Delete("/user/:studentId/related/:relativeId")
	@ResourceFilter(RelativeStudentFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void unlinkRelativeStudent(final HttpServerRequest request) {
		final String studentId = request.params().get("studentId");
		final String relativeId = request.params().get("relativeId");
		userService.unlinkRelativeStudent(relativeId, studentId, defaultResponseHandler(request));
	}

	@Delete("/duplicate/ignore/:userId1/:userId2")
	@ResourceFilter(AdmlOfTwoUsers.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void ignoreDuplicate(final HttpServerRequest request) {
		final String userId1 = request.params().get("userId1");
		final String userId2 = request.params().get("userId2");
		userService.ignoreDuplicate(userId1, userId2, defaultResponseHandler(request));
	}

	@Put("/duplicate/merge/:userId1/:userId2")
	@ResourceFilter(AdmlOfTwoUsers.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void mergeDuplicate(final HttpServerRequest request) {
		final String userId1 = request.params().get("userId1");
		final String userId2 = request.params().get("userId2");
		userService.mergeDuplicate(userId1, userId2, defaultResponseHandler(request));
	}

	@Get("/duplicates")
	@ResourceFilter(AdmlOfStructures.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listDuplicates(final HttpServerRequest request) {
		final List<String> structures = request.params().getAll("structure");
		final boolean inherit = "true".equals(request.params().get("inherit"));
		userService.listDuplicates(new JsonArray(structures.toArray()), inherit, arrayResponseHandler(request));
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
