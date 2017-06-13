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

import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.security.BCrypt;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.neo4j.Neo;
import org.entcore.directory.security.AdmlOfStructuresByExternalId;
import org.entcore.directory.services.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.*;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.bus.BusResponseHandler.busArrayHandler;
import static org.entcore.common.bus.BusResponseHandler.busResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class DirectoryController extends BaseController {

	private Neo neo;
	private JsonObject config;
	private JsonObject admin;
	private SchoolService schoolService;
	private ClassService classService;
	private UserService userService;
	private GroupService groupService;
	private SlotProfileService slotProfileService;

	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		this.neo = new Neo(vertx, eb,log);
		this.config = container.config();
		this.admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
	}

	@Get("/admin-console")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdminFilter.class)
	public void adminConsole(HttpServerRequest request) {
		renderView(request, new JsonObject());
	}

	@Get("/class-admin")
	@SecuredAction(value = "userbook.authent", type = ActionType.AUTHENTICATED)
	public void classAdmin(HttpServerRequest request) {
		renderView(request);
	}

	@Post("/import")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@ResourceFilter(AdmlOfStructuresByExternalId.class)
	@IgnoreCsrf
	public void launchImport(final HttpServerRequest request) {
		final JsonObject json = new JsonObject()
				.putString("action", "import")
				.putString("feeder", request.params().get("feeder"))
				.putString("profile", request.params().get("profile"))
				.putString("charset", request.params().get("charset"))
				.putString("structureExternalId", request.params().get("structureExternalId"));

		eb.send("entcore.feeder", json);
		request.response().end();
	}

	@Post("/transition")
	@SecuredAction("directory.transition")
	@IgnoreCsrf
	public void launchTransition(final HttpServerRequest request) {
		JsonObject t = new JsonObject().putString("action", "transition");
		String structureId = request.params().get("structureExternalId");
		if (structureId != null) {
			t.putString("structureExternalId", structureId);
		}
		eb.send("entcore.feeder", t, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				renderJson(request, event.body());
			}
		});
	}

	@Post("/duplicates/mark")
	@SecuredAction("directory.duplicates.mark")
	@IgnoreCsrf
	public void markDuplicates(final HttpServerRequest request) {
		eb.send("entcore.feeder", new JsonObject().putString("action", "mark-duplicates"), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				renderJson(request, event.body());
			}
		});
	}

	@Post("/export")
	@SecuredAction("directory.export")
	@IgnoreCsrf
	public void launchExport(HttpServerRequest request) {
		eb.send("entcore.feeder", new JsonObject().putString("action", "export"));
		request.response().end();
	}

	@Post("/reinitLogins")
	@SecuredAction("directory.reinit.login")
	@IgnoreCsrf
	public void reinitLogins(HttpServerRequest request) {
		eb.send("entcore.feeder", new JsonObject().putString("action", "reinit-logins"));
		request.response().end();
	}

	@Get("/annuaire")
	@SecuredAction(value = "directory.search.view", type = ActionType.AUTHENTICATED)
	public void annuaire(HttpServerRequest request) {
		renderView(request, null, "annuaire.html", null);
	}

	@Get("/schools")
	@SecuredAction(value = "directory.schools", type = ActionType.AUTHENTICATED)
	public void schools(HttpServerRequest request) {
		renderView(request);
	}

	@Get("/api/ecole")
	@SecuredAction("directory.authent")
	public void school(HttpServerRequest request) {
		neo.send("MATCH (n:Structure) RETURN distinct n.name as name, n.id as id", request.response());
	}

	@Post("/school")
	@SecuredAction("directory.school.create")
	@IgnoreCsrf
	public void createSchool(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject school) {
				schoolService.create(school, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(final Either<String, JsonObject> r) {
						if (r.isRight()) {
							if (r.right().getValue() != null && r.right().getValue().size() > 0) {
								JsonObject j = new JsonObject()
										.putString("action", "initDefaultCommunicationRules")
										.putArray("schoolIds", new JsonArray().add(
												r.right().getValue().getString("id")));
								eb.send("wse.communication", j, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										renderJson(request, r.right().getValue(), 201);
									}
								});
							} else {
								request.response().setStatusCode(404).end();
							}
						} else {
							leftToResponse(request, r.left());
						}
					}
				});
			}
		});
	}

	@Get("/school/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getSchool(final HttpServerRequest request) {
		String schoolId = request.params().get("id");
		schoolService.get(schoolId, notEmptyResponseHandler(request));
	}

	@Post("/class/:schoolId")
	@SecuredAction("directory.class.create")
	@IgnoreCsrf
	public void createClass(final HttpServerRequest request) {
		final String schoolId = request.params().get("schoolId");
		if (schoolId == null || schoolId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject c) {
				classService.create(schoolId, c, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(final Either<String, JsonObject> event) {
						if (event.isRight()) {
							if (event.right().getValue() != null && event.right().getValue().size() > 0) {
								JsonObject j = new JsonObject()
										.putString("action", "initDefaultCommunicationRules")
										.putArray("schoolIds", new JsonArray().add(schoolId));
								eb.send("wse.communication", j);
								String classId = event.right().getValue().getString("id");
								if (classId != null && !classId.trim().isEmpty() &&
										request.params().contains("setDefaultRoles") &&
										config.getBoolean("classDefaultRoles", false)) {
									ApplicationUtils.setDefaultClassRoles(eb, classId,
											new Handler<Message<JsonObject>>() {
												@Override
												public void handle(Message<JsonObject> message) {
													renderJson(request, event.right().getValue(), 201);
												}
											});
								} else {
									renderJson(request, event.right().getValue(), 201);
								}
							} else {
								request.response().setStatusCode(404).end();
							}
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

	@Get("/api/classes")
	@SecuredAction("directory.classes")
	public void classes(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		neo.send("MATCH (n:Structure)<-[:BELONGS]-(m:Class) " +
				"WHERE n.id = {id} " +
				"RETURN distinct m.name as name, m.id as classId, n.id as schoolId",
				params, request.response());
	}

	@Get("/api/personnes")
	@SecuredAction("directory.authent")
	public void people(HttpServerRequest request) {
		List<String> expectedTypes = request.params().getAll("type");
		JsonObject params = new JsonObject();
		params.putString("classId", request.params().get("id"));
		String types = "";
		if (expectedTypes != null && !expectedTypes.isEmpty()) {
			types = "AND p.name IN {expectedTypes} ";
			params.putArray("expectedTypes", new JsonArray(expectedTypes.toArray()));
		}
		neo.send("MATCH (n:Class)<-[:DEPENDS]-(g:ProfileGroup)<-[:IN]-(m:User), "
				+ "g-[:DEPENDS]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) "
				+ "WHERE n.id = {classId} " + types
				+ "RETURN distinct m.id as userId, p.name as type, "
				+ "m.activationCode as code, m.firstName as firstName,"
				+ "m.lastName as lastName, n.id as classId "
				+ "ORDER BY type DESC ", params.toMap(), request.response());
	}

	@Get("/users")
	@SecuredAction("directory.list.users")
	public void users(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		String classId = request.params().get("classId");
		List<String> profiles = request.params().getAll("profile");
		userService.list(structureId, classId, new JsonArray(profiles.toArray()), arrayResponseHandler(request));
	}

	@Get("/api/details")
	@SecuredAction("directory.authent")
	public void details(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", request.params().get("id"));
		neo.send("MATCH (n:User) " +
				"WHERE n.id = {id} " +
				"RETURN distinct n.id as id, n.login as login, n.address as address, n.activationCode as code;"
				, params, request.response());
	}

	@Post("/api/user")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	@IgnoreCsrf
	public void createUser(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String classId = request.formAttributes().get("classId");
				final String structureId = request.formAttributes().get("structureId");
				if ((classId == null || classId.trim().isEmpty()) &&
						(structureId == null || structureId.trim().isEmpty())) {
					badRequest(request);
					return;
				}
				JsonObject user = new JsonObject()
						.putString("firstName", request.formAttributes().get("firstname"))
						.putString("lastName", request.formAttributes().get("lastname"))
						.putString("type", request.formAttributes().get("type"));
				String birthDate = request.formAttributes().get("birthDate");
				if (birthDate != null && !birthDate.trim().isEmpty()) {
					user.putString("birthDate", birthDate);
				}
				List<String> childrenIds = request.formAttributes().getAll("childrenIds");
				user.putArray("childrenIds", new JsonArray(childrenIds.toArray()));
				if (classId != null && !classId.trim().isEmpty()) {
					userService.createInClass(classId, user, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> res) {
							if (res.isRight() && res.right().getValue().size() > 0) {
								JsonObject r = res.right().getValue();
								JsonArray a = new JsonArray().addString(r.getString("id"));
								ApplicationUtils.sendModifiedUserGroup(eb, a, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
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
									}
								});
								renderJson(request, r);
							} else {
								leftToResponse(request, res.left());
							}
						}
					});
				} else {
					userService.createInStructure(structureId, user, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> res) {
							if (res.isRight() && res.right().getValue().size() > 0) {
								JsonObject r = res.right().getValue();
								JsonArray a = new JsonArray().addString(r.getString("id"));
								ApplicationUtils.sendModifiedUserGroup(eb, a, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										JsonObject j = new JsonObject()
												.putString("action", "setDefaultCommunicationRules")
												.putString("schoolId", structureId);
										eb.send("wse.communication", j);
									}
								});
								renderJson(request, r);
							} else {
								leftToResponse(request, res.left());
							}
						}
					});
				}
			}
		});
	}

	@Get("/api/export")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void export(final HttpServerRequest request) {
		String neoRequest = "";
		Map<String, Object> params = new HashMap<>();
		if (request.params().get("id").equals("all")){
			neoRequest =
					"MATCH (m:User)-[:IN]->g " +
					"WHERE NOT(m.activationCode IS NULL) " +
					"OPTIONAL MATCH g-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"RETURN distinct m.lastName as lastName, m.firstName as firstName, " +
					"m.login as login, m.activationCode as activationCode, " +
					"p.name as type " +
					"ORDER BY type, login ";
		} else if (request.params().get("id") != null) {
			neoRequest =
					"MATCH (m:User)-[:IN]->g-[:DEPENDS]->n " +
					"WHERE (n:Structure OR n:Class) AND n.id = {id} AND NOT(m.activationCode IS NULL) " +
					"OPTIONAL MATCH g-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"RETURN distinct m.lastName as lastName, m.firstName as firstName, " +
					"m.login as login, m.activationCode as activationCode, " +
					"p.name as type " +
					"ORDER BY type, login ";
			params.put("id", request.params().get("id"));
		} else {
			notFound(request);
		}
		neo.send(neoRequest, params, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonArray r = Neo.resultToJsonArray(res.body().getObject("result"));
					processTemplate(request, "text/export.txt",
							new JsonObject().putArray("list", r), new Handler<String>() {
						@Override
						public void handle(String export) {
							if (export != null) {
								request.response().putHeader("Content-Type", "application/csv");
								request.response().putHeader("Content-Disposition",
										"attachment; filename=activation_de_comptes.csv");
								request.response().end(export);
							} else {
								renderError(request);
							}
						}
					});
				} else {
					renderError(request);
				}
			}
		});
	}

	public void createSuperAdmin(){
		neo.send("MATCH (n:User)-[:HAS_FUNCTION]->(f:Function { externalId : 'SUPER_ADMIN'}) "
			+ "WHERE n.id = '" + admin.getString("id") + "' "
			+ "WITH count(*) AS exists "
			+ "WHERE exists=0 "
			+ "CREATE (m:User {id:'" + admin.getString("id") + "', "
			+ "externalId:'" + UUID.randomUUID().toString() + "', "
			+ "manual:true, "
			+ "lastName:'"+ admin.getString("firstname") +"', "
			+ "firstName:'"+ admin.getString("lastname") +"', "
			+ "login:'"+ admin.getString("login") +"', "
			+ "displayName:'"+ admin.getString("firstname") +" " + admin.getString("lastname") +"', "
			+ "password:'"+ BCrypt.hashpw(admin.getString("password"), BCrypt.gensalt()) +"'})-[:HAS_FUNCTION]->" +
			"(f:Function { externalId : 'SUPER_ADMIN', name : 'SuperAdmin' })");
		neo.execute("MERGE (u:User {id:'OAuthSystemUser'}) ON CREATE SET u.manual = true", (JsonObject) null, (Handler<Message<JsonObject>>) null);
	}

	@BusAddress("directory")
	public void directoryHandler(final Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		String userId = message.body().getString("userId");
		switch (action) {
			case "usersInProfilGroup":
				boolean itSelf2 = message.body().getBoolean("itself", false);
				String excludeUserId = message.body().getString("excludeUserId");
				userService.list(userId, itSelf2, excludeUserId, responseHandler(message));
				break;
			case "getUser" :
				userService.get(userId, false, BusResponseHandler.busResponseHandler(message));
				break;
			case "getUserInfos" :
				userService.getInfos(userId, BusResponseHandler.busResponseHandler(message));
				break;
			case "list-users":
				JsonArray userIds = message.body().getArray("userIds", new JsonArray());
				JsonArray groupIds = message.body().getArray("groupIds", new JsonArray());
				boolean itSelf = message.body().getBoolean("itself", false);
				String excludeId = message.body().getString("excludeUserId");
				userService.list(groupIds, userIds, itSelf, excludeId, busArrayHandler(message));
				break;
			case "list-structures" :
				schoolService.list(message.body().getArray("fields"), busArrayHandler(message));
				break;
			case "list-groups" :
				String structureId = message.body().getString("structureId");
				String type = message.body().getString("type");
				boolean subGroups = message.body().getBoolean("subGroups", false);
				groupService.list(structureId, type, subGroups, busArrayHandler(message));
				break;
			case "list-adml" :
				String sId = message.body().getString("structureId");
				userService.listAdml(sId, responseHandler(message));
				break;
			case "list-slotprofiles" :
				String structId = message.body().getString("structureId");
				slotProfileService.listSlotProfilesByStructure(structId, busArrayHandler(message));
				break;
			case "list-slots" :
				String slotProfileId = message.body().getString("slotProfileId");
				slotProfileService.listSlots(slotProfileId, busResponseHandler(message));
				break;
		default:
			message.reply(new JsonObject()
				.putString("status", "error")
				.putString("message", "Invalid action."));
		}
	}

	public void setSchoolService(SchoolService schoolService) {
		this.schoolService = schoolService;
	}

	public void setClassService(ClassService classService) {
		this.classService = classService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	private Handler<Either<String, JsonArray>> responseHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonArray>>() {

			@Override
			public void handle(Either<String, JsonArray> res) {
				JsonArray j;
				if (res.isRight()) {
					j = res.right().getValue();
				} else {
					log.warn(res.left().getValue());
					j = new JsonArray();
				}
				message.reply(j);
			}
		};
	}

	public void setGroupService(GroupService groupService) {
		this.groupService = groupService;
	}

	public void setSlotProfileService (SlotProfileService slotProfileService) {
		this.slotProfileService = slotProfileService;
	}
}
