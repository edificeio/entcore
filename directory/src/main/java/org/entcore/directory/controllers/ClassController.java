package org.entcore.directory.controllers;

import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.NotificationHelper;
import fr.wseduc.webutils.Server;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultClassService;
import org.entcore.directory.services.impl.DefaultSchoolService;
import org.entcore.directory.services.impl.DefaultUserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

public class ClassController extends Controller {

	private final ClassService classService;
	private final UserService userService;
	private final SchoolService schoolService;

	public ClassController(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		Neo neo = new Neo(eb,log);
		NotificationHelper notification = new NotificationHelper(vertx, eb, container);
		this.classService = new DefaultClassService(neo, eb);
		this.userService = new DefaultUserService(neo, notification);
		schoolService = new DefaultSchoolService(neo);
	}

	@SecuredAction(value = "class.get", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String classId = request.params().get("classId");
		classService.get(classId, notEmptyResponseHandler(request));
	}

	@SecuredAction(value = "class.update", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				String classId = request.params().get("classId");
				classService.update(classId, body, defaultResponseHandler(request));
			}
		});
	}

	@SecuredAction(value = "class.user.create", type = ActionType.RESOURCE)
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
							applyComRulesAndRegistryEvent(classId, new JsonArray().add(userId));
							if (container.config().getBoolean("createdUserEmail", false) &&
									request.params().contains("sendCreatedUserEmail")) {
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

	@SecuredAction(value = "class.user.find", type = ActionType.RESOURCE)
	public void findUsers(final HttpServerRequest request) {
		final String classId = request.params().get("classId");
		List<UserService.UserType> types = new ArrayList<>();
		for (String t: request.params().getAll("type")) {
			try {
				types.add(UserService.UserType.valueOf(t));
			} catch (Exception e) {
				badRequest(request);
				return;
			}
		}
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
											request.response().end(export);
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
		classService.findUsers(classId, types.toArray(new UserService.UserType[types.size()]), handler);
	}

	@SecuredAction(value = "class.csv", type = ActionType.RESOURCE)
	public void csv(final HttpServerRequest request) {
		request.expectMultiPart(true);
		final String classId = request.params().get("classId");
		final String userType = request.params().get("userType");
		if (classId == null || classId.trim().isEmpty() ||
				(!"Student".equalsIgnoreCase(userType) && !"Relative".equalsIgnoreCase(userType))) {
			badRequest(request);
			return;
		}
		request.uploadHandler(new Handler<HttpServerFileUpload>() {
			@Override
			public void handle(final HttpServerFileUpload event) {
				final Buffer buff = new Buffer();
				event.dataHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer event) {
						buff.appendBuffer(event);
					}
				});
				event.endHandler(new Handler<Void>() {
					@Override
					public void handle(Void end) {
						String ut = Character.toUpperCase(userType.charAt(0)) +
								userType.substring(1).toLowerCase();
						JsonObject j = new JsonObject()
								.putString("action", "csvClass" + ut)
								.putString("classId", classId)
								.putString("csv", buff.toString("ISO-8859-1"));
						Server.getEventBus(vertx).send(container.config().getString("feeder",
								"entcore.feeder"), j, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								JsonArray r = message.body().getArray("results");
								if ("ok".equals(message.body().getString("status")) && r != null) {
									JsonArray users = new JsonArray();
									for (int i = 0; i < r.size(); i++) {
										JsonArray s = r.get(i);
										if (s != null && s.size() == 1) {
											String u = ((JsonObject) s.get(0)).getString("id");
											if (u != null) {
												users.addString(u);
											}
										}
									}
									if (users.size() > 0) {
										ClassController.this.applyComRulesAndRegistryEvent(classId, users);
										request.response().end();
									} else {
										renderJson(request, new JsonObject()
												.putString("message", "import.invalid." + userType.toLowerCase()), 400);
									}
								} else {
									renderJson(request, message.body(), 400);
								}
							}
						});
					}
				});
			}
		});
	}

	@SecuredAction(value = "class.add.user", type = ActionType.RESOURCE)
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

	@SecuredAction(value = "class.apply.rules", type = ActionType.RESOURCE)
	public void applyComRulesAndRegistryEvent(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject body) {
				final String classId = request.params().get("classId");
				JsonArray userIds = body.getArray("userIds");
				if (userIds != null) {
					ClassController.this.applyComRulesAndRegistryEvent(classId, userIds);
					request.response().end();
				} else {
					badRequest(request);
				}
			}
		});
	}

	private void applyComRulesAndRegistryEvent(String classId, JsonArray userIds) {
		schoolService.getByClassId(classId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> s) {
				if (s.isRight()) {
					JsonObject j = new JsonObject()
							.putString("action", "setDefaultCommunicationRules")
							.putString("schoolId", s.right().getValue().getString("id"));
					eb.send("wse.communication", j);
				}
			}
		});
		ApplicationUtils.publishModifiedUserGroup(eb, userIds);
	}

}
