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
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class ClassController extends BaseController {

	private ClassService classService;
	private UserService userService;
	private SchoolService schoolService;
	private ConversationNotification conversationNotification;
	public static final List<String> csvMimeTypes = Arrays.asList("text/comma-separated-values", "text/csv",
			"application/csv", "application/excel", "application/vnd.ms-excel", "application/vnd.msexcel",
			"text/anytext", "text/plain");

	@Get("/class/:classId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String classId = request.params().get("classId");
		classService.get(classId, notEmptyResponseHandler(request));
	}

	@Put("/class/:classId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String classId = request.params().get("classId");
				classService.update(classId, body, defaultResponseHandler(request));
			}
		});
	}

	@Post("/class/:classId/user")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void createUser(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String classId = request.params().get("classId");
				userService.createInClass(classId, body, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> r) {
						if (r.isRight()) {
							final String userId = r.right().getValue().getString("id");
							boolean notify = container.config().getBoolean("createdUserEmail", false) &&
									request.params().contains("sendCreatedUserEmail");
							initPostCreate(classId, new JsonArray().add(userId), notify, request);
							if (notify) {
								userService.sendUserCreatedEmail(request, userId,
										new Handler<Either<String, Boolean>>() {
									@Override
									public void handle(Either<String, Boolean> e) {
										if (e.isRight()) {
											log.info("User " + userId + " created email sent.");
										} else {
											log.error("Error sending user " + userId + " created email : " +
											e.left().getValue());
										}
									}
								});
							}
							renderJson(request, r.right().getValue(), 201);
						} else {
							renderJson(request, new JsonObject().putString("error", r.left().getValue()), 400);
						}
					}
				});
			}
		});
	}

	@Get("/class/:classId/users")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void findUsers(final HttpServerRequest request) {
		final String classId = request.params().get("classId");
		JsonArray types = new JsonArray(request.params().getAll("type").toArray());
	 	Handler<Either<String, JsonArray>> handler;
		if ("csv".equals(request.params().get("format"))) {
			handler = new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> r) {
					if (r.isRight()) {
						processTemplate(request, "text/export.txt",
								new JsonObject().putArray("list", r.right().getValue()), new Handler<String>() {
							@Override
							public void handle(final String export) {
								if (export != null) {
									classService.get(classId, new Handler<Either<String, JsonObject>>() {
										@Override
										public void handle(Either<String, JsonObject> c) {
											String name = classId;
											if (c.isRight()) {
												name = c.right().getValue().getString("name", name)
														.replaceAll("\\s+", "_");
											}
											request.response().putHeader("Content-Type", "application/csv");
											request.response().putHeader("Content-Disposition",
													"attachment; filename=" + name + ".csv");
											request.response().end('\ufeff' + export);
										}
									});
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
		classService.findUsers(classId, types, handler);
	}


	@Put("/class/:classId/add/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addUser(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String classId = request.params().get("classId");
					final String userId = request.params().get("userId");
					classService.addUser(classId, userId, user, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> res) {
							if (res.isRight()) {
								String schoolId = res.right().getValue().getString("schoolId");
								JsonObject j = new JsonObject()
										.putString("action", "setDefaultCommunicationRules")
										.putString("schoolId", schoolId);
								eb.send("wse.communication", j);
								JsonArray a = new JsonArray().addString(userId);
								ApplicationUtils.publishModifiedUserGroup(eb, a);
								renderJson(request, res.right().getValue());
							} else {
								renderJson(request, new JsonObject().putString("error", res.left().getValue()), 400);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/class/:classId/apply")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void applyComRulesAndRegistryEvent(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String classId = request.params().get("classId");
				JsonArray userIds = body.getArray("userIds");
				if (userIds != null) {
					ClassController.this.initPostCreate(classId, userIds);
					request.response().end();
				} else {
					badRequest(request);
				}
			}
		});
	}

	private void initPostCreate(String classId, JsonArray userIds) {
		initPostCreate(classId, userIds, false, null);
	}

	private void initPostCreate(final String classId, final JsonArray userIds, final boolean welcomeMessage,
			final HttpServerRequest request) {
		ApplicationUtils.sendModifiedUserGroup(eb, userIds, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				schoolService.getByClassId(classId, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> s) {
						if (s.isRight()) {
							JsonObject j = new JsonObject()
									.putString("action", "setDefaultCommunicationRules")
									.putString("schoolId", s.right().getValue().getString("id"));
							eb.send("wse.communication", j, new Handler<Message<JsonObject>>() {
								public void handle(Message<JsonObject> event) {
									if("error".equals(event.body().getString("status", ""))){
										log.error("[initPostCreate] Set communication rules failed.");
									} else if(welcomeMessage){
										JsonObject params = new JsonObject();
										conversationNotification.notify(request, "", userIds, null,
											"welcome.subject", "email/welcome.html", params,
											new Handler<Either<String, JsonObject>>() {
												public void handle(Either<String, JsonObject> r) {
													if (r.isLeft()) {
														log.error(r.left().getValue());
													}
											  }
											});
									}
								}
							});
						}
					}
				});

		   }
		});
	}

	@Put("/class/:classId/link/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void linkUser(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String classId = request.params().get("classId");
		classService.link(classId, userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					if (r.right().getValue() != null && r.right().getValue().size() > 0) {
						initPostCreate(classId, new JsonArray().add(userId));
						renderJson(request, r.right().getValue(), 200);
					} else {
						notFound(request);
					}
				} else {
					renderJson(request, new JsonObject().putString("error", r.left().getValue()), 400);
				}
			}
		});
	}

	@Delete("/class/:classId/unlink/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void unlinkUser(final HttpServerRequest request) {
		final String classId = request.params().get("classId");
		final String userId = request.params().get("userId");
		classService.unlink(classId, userId, notEmptyResponseHandler(request));
	}

	@Get("/class/admin/list")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listAdmin(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					final String structureId = request.params().get("structureId");
					classService.listAdmin(structureId, user, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	public void setClassService(ClassService classService) {
		this.classService = classService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public void setSchoolService(SchoolService schoolService) {
		this.schoolService = schoolService;
	}

	public void setConversationNotification(ConversationNotification conversationNotification) {
		this.conversationNotification = conversationNotification;
	}

}
