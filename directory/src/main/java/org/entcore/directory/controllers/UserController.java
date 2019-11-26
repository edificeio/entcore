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

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import static org.entcore.common.user.SessionAttributes.PERSON_ATTRIBUTE;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import fr.wseduc.webutils.security.SecureHttpServerRequest;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.directory.pojo.Users;
import org.entcore.directory.security.*;
import org.entcore.directory.services.UserBookService;
import org.entcore.directory.services.UserService;
import org.vertx.java.core.http.RouteMatcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class UserController extends BaseController {

	private UserService userService;
	private UserBookService userBookService;
	private TimelineHelper notification;
	private static final int MOTTO_MAX_LENGTH = 75;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
	}

	@Put("/user/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void update(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos user) {
						String userId = request.params().get("userId");

						//User name modification prevention for non-admins.
						if(!user.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN) &&
								!user.getFunctions().containsKey(DefaultFunctions.ADMIN_LOCAL) &&
								!user.getFunctions().containsKey(DefaultFunctions.CLASS_ADMIN)){
							body.remove("lastName");
							body.remove("firstName");
						}
						userService.update(userId, body, notEmptyResponseHandler(request));


						UserUtils.removeSessionAttribute(eb, userId, PERSON_ATTRIBUTE, null);
					}
				});
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
				String motto = body.getString("motto");
				if( motto != null && motto.length() > MOTTO_MAX_LENGTH){
					badRequest(request);
					return;
				}
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
									.put("error", event.left().getValue());
							renderJson(request, error, 400);
						}
					}
				});
			}
		});
	}

	@Get("/user/:userId")
	@ResourceFilter(UserAccessOrVisible.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void get(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		boolean getManualGroups = Boolean.parseBoolean(request.params().get("manual-groups"));
		if ("true".equals(((SecureHttpServerRequest) request).getAttribute("visibleCheck"))) {
			final JsonArray filter = new JsonArray()
					.add("activationCode").add("firstName").add("lastName").add("mobile").add("mobilePhone")
					.add("lastLogin").add("created").add("modified").add("ine").add("email").add("emailAcademy")
					.add("workPhone").add("homePhone").add("country").add("zipCode").add("address").add("postbox")
					.add("city").add("otherNames").add("title");
			userService.get(userId, getManualGroups, filter, notEmptyResponseHandler(request));
		} else {
			userService.get(userId, getManualGroups, notEmptyResponseHandler(request));
		}
	}

	@Get("/user/:userId/groups")
	@ResourceFilter(UserAccessOrVisible.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getGroups(final HttpServerRequest request) {
		String userId = request.params().get("userId");
		userService.getGroups(userId, arrayResponseHandler(request));
	}

	@Get("/myinfos")
	@SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
	public void myinfos(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			userService.get(user.getUserId(), false, defaultResponseHandler(request));
		});
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
				final JsonObject j = new JsonObject().put("picture", p);
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
						.put("uri", pathPrefix + "/annuaire#" + user.getUserId() + "#" + user.getType())
						.put("username", user.getUsername())
						.put("motto", motto)
						.put("moodImg", mood);
				if (mood != null && !mood.trim().isEmpty() && !mood.equals("default")) {
					notification.notifyTimeline(request, "userbook.userbook_mood", user, userIds,
							user.getUserId() + System.currentTimeMillis() + "mood", params);
				}
				if (motto != null && !motto.trim().isEmpty()) {
					notification.notifyTimeline(request, "userbook.userbook_motto", user, userIds,
							user.getUserId() + System.currentTimeMillis() + "motto", params);
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

	@Post("/user/delete")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(TeacherOfUser.class)
	public void deleteByPost(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			@SuppressWarnings("unchecked")
			public void handle(JsonObject event) {
				if (event != null) {
					userService.delete(event.getJsonArray("users", new fr.wseduc.webutils.collections.JsonArray()).getList(), defaultResponseHandler(request));
				} else {
					badRequest(request, "invalid.json");
				}
			}
		});
	}

	@Put("/restore/user")
	@ResourceFilter(AnyAdminOfUser.class)
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
					JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("profile"));
					final String profile = (types != null && types.size() > 0) ? types.getString(0) : "All";
					final String filterActive = request.params().get("filterActive");
					final String exportType = request.params().get("type") == null ? "" : request.params().get("type");
					final String format = request.params().get("format");
					Handler<Either<String, JsonArray>> handler;
					if(format == null){
						handler = arrayResponseHandler(request);
					} else {
						handler = new Handler<Either<String, JsonArray>>() {
							@Override
							public void handle(Either<String, JsonArray> r) {
								if (r.isRight()) {
									processTemplate(request, new JsonObject().put("list", r.right().getValue()).put(profile,true),  "text/export" + exportType + ".id.txt", false, new Handler<String>() {
										@Override
										public void handle(final String export) {
											if (export != null) {
												String filename = request.params().get("filename") != null ?
														request.params().get("filename") : "export"+exportType+"."+format;
												if ("xml".equals(format)) {
													request.response().putHeader("Content-Type", "text/xml");
												} else {
													request.response().putHeader("Content-Type", "application/csv");
												}
												request.response().putHeader("Content-Disposition",
														"attachment; filename="+filename);
												request.response().end('\ufeff' + export);
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
					}
					userService.listAdmin(structureId, classId, null, types, filterActive, null, user, handler);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/user/function/:userId")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AddFunctionFilter.class)
	@IgnoreCsrf
	public void addFunction(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, pathPrefix + "addFunction", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				userService.addFunction(userId, event.getString("functionCode"),
						event.getJsonArray("scope"), event.getString("inherit", ""), r -> {
							if (r.isRight()) {
								final String groupId = (String) r.right().getValue().remove("groupId");
								if (isNotEmpty(groupId)) {
									JsonObject j = new JsonObject()
											.put("action", "setCommunicationRules")
											.put("groupId", groupId);
									eb.send("wse.communication", j);
								}
								renderJson(request, r.right().getValue());
							} else {
								badRequest(request, r.left().getValue());
							}
						});
			}
		});
	}

	@Post("/:structure/user/:userId/headteacher")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructures.class)
	@IgnoreCsrf
	public void addHeadTeacherManual(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, pathPrefix + "addHeadTeacher", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				userService.addHeadTeacherManual(userId, event.getString("structureExternalId"), event.getString("classExternalId"),
						event.getString("structureName"), defaultResponseHandler(request));
			}
		});
	}

	@Put("/:structure/user/:userId/headteacher")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructures.class)
	public void updateHeadTeacherManual(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		bodyToJson(request, pathPrefix + "updateHeadTeacher", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				userService.updateHeadTeacherManual(userId, event.getString("structureExternalId"), event.getString("classExternalId"), defaultResponseHandler(request));
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

	@Get("/user/:userId/functions")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listFunctions(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		userService.listFunctions(userId, arrayResponseHandler(request));
	}

	@Get("/user/:userId/children")
	@ResourceFilter(UserAccessOrVisible.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listChildren(final HttpServerRequest request) {
		final String userId = request.params().get("userId");
		userService.listChildren(userId, arrayResponseHandler(request));
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
							.put("action", "setCommunicationRules")
							.put("groupId", groupId);
					eb.send("wse.communication", j);
					JsonArray a = new fr.wseduc.webutils.collections.JsonArray().add(userId);
					ApplicationUtils.publishModifiedUserGroup(eb, a);
					renderJson(request, res.right().getValue());
				} else {
					renderJson(request, new JsonObject().put("error", res.left().getValue()), 400);
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
					final JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("profile"));
					final String groupId = request.params().get("groupId");
					final String nameFilter = request.params().get("name");
					final String filterActive = request.params().get("filterActive");

					userService.listAdmin(structureId, classId, groupId, types, filterActive, nameFilter, user, arrayResponseHandler(request));
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
					JsonArray structures = res.right().getValue().getJsonArray("structures");
					JsonObject j = new JsonObject()
							.put("action", "setMultipleDefaultCommunicationRules")
							.put("schoolIds", structures);
					eb.send("wse.communication", j);
					JsonArray a = new fr.wseduc.webutils.collections.JsonArray().add(relativeId);
					ApplicationUtils.publishModifiedUserGroup(eb, a);
					if (structures == null || structures.size() == 0) {
						notFound(request, "user.not.found");
					} else {
						ok(request);
					}
				} else {
					renderJson(request, new JsonObject().put("error", res.left().getValue()), 400);
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

	@Delete("/duplicate/ignore/:userId/:userId2")
	@ResourceFilter(AdmlOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void ignoreDuplicate(final HttpServerRequest request) {
		final String userId1 = request.params().get("userId");
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
		userService.listDuplicates(new fr.wseduc.webutils.collections.JsonArray(structures), inherit, arrayResponseHandler(request));
	}


	@Get("/user/structures/list")
	@ResourceFilter(AdmlOfStructuresByUAI.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listUserInStructuresByUAI(final HttpServerRequest request) {
		final String format = request.params().get("format");
		final List<String> structures = request.params().getAll("uai");

		JsonArray fields = new fr.wseduc.webutils.collections.JsonArray().add("externalId").add("lastName").add("firstName").add("login");
		if ("true".equalsIgnoreCase(request.params().get("administrativeStructure"))) {
			fields.add("administrativeStructure");
		}
		JsonArray types = new fr.wseduc.webutils.collections.JsonArray(request.params().getAll("type"));

		boolean isExportFull = false;
		String isExportFullParameter = request.params().get("full");
		if(null != isExportFullParameter
				&& !isExportFullParameter.isEmpty()
				&& "true".equals(isExportFullParameter)){
			isExportFull = true;
		}

		if ("XML".equalsIgnoreCase(format)) {
			userService.listByUAI(structures, types, isExportFull, fields, new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> event) {
					if (event.isRight()) {
						JsonArray r = event.right().getValue();
						ObjectMapper mapper = new ObjectMapper();
						try {
							List<Users.User> users = mapper.readValue(r.encode(), new TypeReference<List<Users.User>>(){});
							StringWriter response = new StringWriter();
							JAXBContext context = JAXBContext.newInstance(Users.class);
							Marshaller marshaller = context.createMarshaller();
							marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
							marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
							marshaller.marshal(new Users(users), response);
							request.response().putHeader("content-type", "application/xml");
							request.response().end(response.toString());
						} catch (IOException | JAXBException e) {
							log.error(e.toString(), e);
							request.response().setStatusCode(500);
							request.response().end(e.getMessage());
						}
					} else {
						leftToResponse(request, event.left());
					}
				}
			});
		} else {
			userService.listByUAI(structures, types, isExportFull, fields, arrayResponseHandler(request));
		}
	}

	@Post("/duplicate/generate/mergeKey/:userId")
	@ResourceFilter(AdmlOfUser.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void generateMergeKeyByAdml(final HttpServerRequest request) {
		userService.generateMergeKey(request.params().get("userId"), notEmptyResponseHandler(request));
	}

	@Get("/duplicate/user/mergeKey")
	@SecuredAction("user.generate.merge.key")
	public void generateMergeKey(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos event) {
				if (event != null) {
					userService.generateMergeKey(event.getUserId(), notEmptyResponseHandler(request));
				} else {
					unauthorized(request, "user.not.found");
				}
			}
		});
	}

	@Post("/duplicate/user/mergeByKey")
	@SecuredAction("user.merge.by.key")
	public void mergeByKey(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(UserInfos event) {
						if (event != null) {
							userService.mergeByKey(event.getUserId(), body, notEmptyResponseHandler(request));
						} else {
							unauthorized(request, "user.not.found");
						}
					}
				});
			}
		});
	}

	@Get("/allowLoginUpdate")
	@SecuredAction("user.allow.login.update")
	public void allowLoginUpdate(final HttpServerRequest request) {
		// This route is used to create user.allow.login.update Workflow right, nothing to do
		request.response().end();
	}

	@Get("/user/level/list")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(SuperAdminFilter.class)
	public void listByLevel(HttpServerRequest request) {
		final boolean stream = "application/x-ndjson".equals(request.headers().get("Accept"));
		final Handler<Either<String, JsonArray>> h;
		if (stream) {
			h = r -> {
				if (r.isRight()) {
					if (!request.response().isChunked()) {
						request.response().setChunked(true);
					}
					r.right().getValue().stream()
							.forEach(o -> request.response().write(((JsonObject) o).encode() + "\n"));
				} else {
					request.response().end(r.left().getValue());
				}
			};
		} else {
			h = arrayResponseHandler(request);
		}
		userService.listByLevel(request.params().get("level"), request.params().get("notLevel"),
				request.params().get("profile"), stream, h);
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
