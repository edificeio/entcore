package org.entcore.directory.controllers;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import java.util.*;

import fr.wseduc.security.ActionType;
import fr.wseduc.webutils.Either;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import org.entcore.directory.services.impl.DefaultClassService;
import org.entcore.directory.services.impl.DefaultSchoolService;
import org.entcore.directory.services.impl.DefaultUserService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.collections.Joiner;

import org.entcore.directory.profils.DefaultProfils;
import org.entcore.directory.profils.Profils;
import fr.wseduc.webutils.Controller;
import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.security.BCrypt;
import fr.wseduc.security.SecuredAction;


public class DirectoryController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject admin;
	private Profils p;
	private final SchoolService schoolService;
	private final ClassService classService;
	private final UserService userService;

	public DirectoryController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.neo = new Neo(eb,log);
			this.config = config;
			this.admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
			this.p = new DefaultProfils(neo);
			this.schoolService = new DefaultSchoolService(neo, eb);
			this.classService = new DefaultClassService(neo, eb);
			this.userService = new DefaultUserService(neo, null, eb);
		}

	@SecuredAction("directory.view")
	public void directory(HttpServerRequest request) {
		renderView(request, new JsonObject());
	}

	@SecuredAction("directory.import")
	public void launchImport(HttpServerRequest request) {
		eb.send("entcore.feeder", new JsonObject().putString("action", "import"));
		request.response().end();
	}

	@SecuredAction(value = "directory.search.view", type = ActionType.AUTHENTICATED)
	public void annuaire(HttpServerRequest request) {
		renderView(request, null, "annuaire.html", null);
	}

	@SecuredAction("directory.authent")
	public void school(HttpServerRequest request) {
		neo.send("MATCH (n:Structure) RETURN distinct n.name as name, n.id as id", request.response());
	}

	@SecuredAction("directory.school.create")
	public void createSchool(final HttpServerRequest request) {
		bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject school) {
				schoolService.create(school, notEmptyResponseHandler(request, 201));
			}
		});
	}

	@SecuredAction("directory.school.get")
	public void getSchool(final HttpServerRequest request) {
		String schoolId = request.params().get("id");
		schoolService.get(schoolId, notEmptyResponseHandler(request));
	}

	@SecuredAction("directory.class.create")
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

	@SecuredAction("directory.classes")
	public void classes(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		neo.send("MATCH (n:Structure)<-[:BELONGS]-(m:Class) " +
				"WHERE n.id = {id} " +
				"RETURN distinct m.name as name, m.id as classId, n.id as schoolId",
				params, request.response());
	}

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

	@SecuredAction("directory.list.users")
	public void users(HttpServerRequest request) {
		String structureId = request.params().get("structureId");
		String classId = request.params().get("classId");
		List<String> profiles = request.params().getAll("profile");
		userService.list(structureId, classId, new JsonArray(profiles.toArray()), arrayResponseHandler(request));
	}

	@SecuredAction("directory.authent")
	public void details(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", request.params().get("id"));
		neo.send("MATCH (n:User) " +
				"WHERE n.id = {id} " +
				"RETURN distinct n.login as login, n.address as address, n.activationCode as code;"
				, params, request.response());
	}

	@SecuredAction("directory.create.user")
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
								JsonArray a = new JsonArray().addString(r.getString("id"));
								ApplicationUtils.publishModifiedUserGroup(eb, a);
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
								JsonObject j = new JsonObject()
										.putString("action", "setDefaultCommunicationRules")
										.putString("schoolId", structureId);
								eb.send("wse.communication", j);
								JsonArray a = new JsonArray().addString(r.getString("id"));
								ApplicationUtils.publishModifiedUserGroup(eb, a);
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

	@SecuredAction("directory.export")
	public void export(final HttpServerRequest request) {
		String neoRequest = "";
		Map<String, Object> params = new HashMap<>();
		if (request.params().get("id").equals("all")){
			neoRequest = "MATCH (m:User) " +
					"WHERE NOT(m.activationCode IS NULL) "
					+ "RETURN distinct m.lastName as lastName, m.firstName as firstName, "
					+ "m.login as login, m.activationCode as activationCode, "
					+ "HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User')) as type "
					+ "ORDER BY type, login ";
		} else if (request.params().get("id") != null) {
			neoRequest =
					"MATCH (m:User)-[:IN]->g-[:DEPENDS]->n " +
					"OPTIONAL MATCH g-[:DEPENDS*0..1]->(pg:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"WHERE (n:Structure OR n:Class) AND n.id = {id} AND NOT(m.activationCode IS NULL) " +
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
		neo.send("MATCH (n:User)-[:IN]->(fg:FunctionGroup)-[:HAS_FUNCTION]->(f:Function { externalId : 'SUPER_ADMIN'}) "
			+ "WHERE n.id = '" + admin.getString("id") + "' "
			+ "WITH count(*) AS exists "
			+ "WHERE exists=0 "
			+ "CREATE (m:User {id:'" + admin.getString("id") + "', "
			+ "externalId:'" + UUID.randomUUID().toString() + "', "
			+ "lastName:'"+ admin.getString("firstname") +"', "
			+ "firstName:'"+ admin.getString("lastname") +"', "
			+ "login:'"+ admin.getString("login") +"', "
			+ "displayName:'"+ admin.getString("firstname") +" " + admin.getString("lastname") +"', "
			+ "password:'"+ BCrypt.hashpw(admin.getString("password"), BCrypt.gensalt()) +"'})" +
			"-[:IN]->(fg:FunctionGroup)-[:HAS_FUNCTION]->" +
			"(f:Function { externalId : 'SUPER_ADMIN', name : 'SuperAdmin' })");
	}

	public void directoryHandler(final Message<JsonObject> message) {
		String action = message.body().getString("action");
		switch (action) {
		case "groups":
			Object[] filterTypes = (message.body().getArray("types") != null) ?
					message.body().getArray("types").toArray() : null;
			p.listGroupsProfils(filterTypes, message.body().getString("schoolId"),
					new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject res) {
					message.reply(res);
				}
			});
			break;
		default:
			message.reply(new JsonObject()
				.putString("status", "error")
				.putString("message", "Invalid action."));
		}
	}

}