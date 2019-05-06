/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.ConversationNotification;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.security.AdmlOfStructureOrClassOrTeachOfUser;
import org.entcore.directory.security.TeacherInAllStructure;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getUserInfos;

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
							boolean notify = config.getBoolean("createdUserEmail", false) &&
									request.params().contains("sendCreatedUserEmail");
							initPostCreate(classId, new fr.wseduc.webutils.collections.JsonArray().add(userId), notify, request);
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
							renderJson(request, new JsonObject().put("error", r.left().getValue()), 400);
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
		JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("type"));
		boolean collectRelative = "true".equals(request.params().get("collectRelative"));
	 	Handler<Either<String, JsonArray>> handler;
		if ("csv".equals(request.params().get("format"))) {
			handler = new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> r) {
					if (r.isRight()) {
						processTemplate(request, "text/export.txt",
								new JsonObject().put("list", r.right().getValue()), new Handler<String>() {
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
						renderJson(request, new JsonObject().put("error", r.left().getValue()), 400);
					}
				}
			};
		} else {
			handler = arrayResponseHandler(request);
		}
		classService.findUsers(classId, types, collectRelative, handler);
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
										.put("action", "setDefaultCommunicationRules")
										.put("schoolId", schoolId);
								eb.send("wse.communication", j);
								JsonArray a = new fr.wseduc.webutils.collections.JsonArray().add(userId);
								ApplicationUtils.publishModifiedUserGroup(eb, a);
								renderJson(request, res.right().getValue());
							} else {
								renderJson(request, new JsonObject().put("error", res.left().getValue()), 400);
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
				JsonArray userIds = body.getJsonArray("userIds");
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
		ApplicationUtils.sendModifiedUserGroup(eb, userIds, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				schoolService.getByClassId(classId, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> s) {
						if (s.isRight()) {
							JsonObject j = new JsonObject()
									.put("action", "setDefaultCommunicationRules")
									.put("schoolId", s.right().getValue().getString("id"));
							eb.send("wse.communication", j, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
							}));
						}
					}
				});

		   }
		}));
	}

	@Put("/class/:classId/link/:userId")
	@ResourceFilter(AdmlOfStructureOrClassOrTeachOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void linkUser(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		final String classId = request.params().get("classId");
		classService.link(classId, userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					if (r.right().getValue() != null && r.right().getValue().size() > 0) {
						initPostCreate(classId, new fr.wseduc.webutils.collections.JsonArray().add(userId));
						renderJson(request, r.right().getValue(), 200);
					} else {
						notFound(request);
					}
				} else {
					renderJson(request, new JsonObject().put("error", r.left().getValue()), 400);
				}
			}
		});
	}

	@Put("/class/:classId/link")
	@ResourceFilter(AdmlOfStructureOrClassOrTeachOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void linkUsers(final HttpServerRequest request) {
		bodyToJson(request, json ->{
			final JsonArray userIds = json.getJsonArray("ids");
			final String classId = request.params().get("classId");
			classService.link(classId, userIds, either -> {
					if (either.isRight()) {
						if (either.right().getValue() != null && either.right().getValue().size() > 0) {
							initPostCreate(classId, userIds);
							renderJson(request, either.right().getValue());
						} else {
							notFound(request);
						}
					} else {
						renderJson(request, new JsonObject().put("error", either.left().getValue()), 400);
					}
			});
		});
	}

	@Put("/class/:classId/unlink")
	@ResourceFilter(AdmlOfStructureOrClassOrTeachOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void unlinkUsers(final HttpServerRequest request) {
		bodyToJson(request, json ->{
			final String classId = request.getParam("classId");
			final JsonArray userIds = json.getJsonArray("ids");
			final JsonArray classIds =new JsonArray();
			for(int i = 0 ; i < userIds.size(); i ++){
				classIds.add(classId);
			}
			classService.unlink(classIds, userIds, arrayResponseHandler(request));
		});
	}

	@Put("/class/:classId/change")
	@ResourceFilter(AdmlOfStructureOrClassOrTeachOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void changeUserClasses(final HttpServerRequest request) {
		bodyToJson(request, json ->{
			final String classId = request.params().get("classId");
			final JsonArray userIds = json.getJsonArray("ids");
			final JsonArray classIds =json.getJsonArray("classIds");
			classService.link(classId, userIds, event->{
				if (event.isRight()) {
					classService.unlink(classIds, userIds, arrayResponseHandler(request));
				} else {
					JsonObject error = new JsonObject().put("error", event.left().getValue());
					Renders.renderJson(request, error, 400);
				}
			});
		});
	}

	@Delete("/class/:classId/unlink/:userId")
	@ResourceFilter(AdmlOfStructureOrClassOrTeachOfUser.class)
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

	@Get("/class/users/detached")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void getDetachedUsers(HttpServerRequest request) {
		getUserInfos(eb, request, user -> {
			if (user != null) {
				List<String> structureIds = request.params().getAll("structureId");
				classService.listDetachedUsers(new JsonArray(structureIds), user, res -> {
					renderJson(request, res);
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Get("/class/users/visibles")
	@SecuredAction(value = "", type = ActionType.AUTHENTICATED)
	public void visibleUsers(final HttpServerRequest request) {
		getUserInfos(eb, request, user -> {
			if (user != null) {
				String classId = request.params().get("classId");
				boolean collectRelative = "true".equals(request.params().get("collectRelative"));
				classService.findVisibles(user, classId, collectRelative, res->{
					renderJson(request,res);
				});
			} else {
				unauthorized(request);
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
