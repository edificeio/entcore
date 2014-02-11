package org.entcore.directory.controllers;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.appregistry.AppRegistryEvents.APP_REGISTRY_PUBLISH_ADDRESS;
import static org.entcore.common.appregistry.AppRegistryEvents.PROFILE_GROUP_ACTIONS_UPDATED;
import static org.entcore.common.appregistry.AppRegistryEvents.USER_GROUP_UPDATED;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import static org.entcore.directory.be1d.BE1DConstants.*;

import java.util.*;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.impl.DefaultClassService;
import org.entcore.directory.services.impl.DefaultSchoolService;
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

import org.entcore.datadictionary.dictionary.DefaultDictionary;
import org.entcore.datadictionary.dictionary.Dictionary;
import org.entcore.datadictionary.generation.ActivationCodeGenerator;
import org.entcore.datadictionary.generation.DisplayNameGenerator;
import org.entcore.datadictionary.generation.IdGenerator;
import org.entcore.datadictionary.generation.LoginGenerator;
import org.entcore.directory.be1d.BE1D;
import org.entcore.directory.profils.DefaultProfils;
import org.entcore.directory.profils.Profils;
import org.entcore.directory.users.UserQueriesBuilder;
import fr.wseduc.webutils.Controller;
import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.security.BCrypt;
import fr.wseduc.security.SecuredAction;


public class DirectoryController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject admin;
	private Dictionary d;
	private Profils p;
	private final ActivationCodeGenerator activationGenerator;
	private final IdGenerator idGenerator;
	private final LoginGenerator loginGenerator;
	private final DisplayNameGenerator displayNameGenerator;
	private final SchoolService schoolService;
	private final ClassService classService;

	public DirectoryController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.neo = new Neo(eb,log);
			this.config = config;
			this.d = new DefaultDictionary(vertx, container, "aaf-dictionary.json");
			this.admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
			this.p = new DefaultProfils(neo);
			this.schoolService = new DefaultSchoolService(neo);
			this.classService = new DefaultClassService(neo, eb);
			loginGenerator = new LoginGenerator();
			activationGenerator = new ActivationCodeGenerator();
			idGenerator = new IdGenerator();
			displayNameGenerator = new DisplayNameGenerator();
			loadUsedLogin();
		}

	private void loadUsedLogin() {
		String query = "MATCH (u:User) RETURN u.login as login";
		neo.send(query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
					Set<String> login = new HashSet<>();
					JsonObject res = event.body().getObject("result");
					for (String attr : res.getFieldNames()) {
						login.add(res.getObject(attr).getString("login", ""));
					}
					LoginGenerator.setUsedLogin(login);
				}
			}
		});
	}

	@SecuredAction("directory.view")
	public void directory(HttpServerRequest request) {
		renderView(request, new JsonObject());
	}

	@SecuredAction("directory.be1d")
	public void testBe1d(final HttpServerRequest r) {
		new BE1D(vertx, container, config.getString("test-be1d-folder","/opt/one/be1d")).importPorteur(
				new Handler<JsonArray>() {
					@Override
					public void handle(JsonArray m) {
						renderJson(r, m);
					}
				});
	}

	@SecuredAction("directory.authent")
	public void school(HttpServerRequest request) {
		neo.send("MATCH (n:School) RETURN distinct n.name as name, n.id as id", request.response());
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
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							if (event.right().getValue() != null && event.right().getValue().size() > 0) {
								String classId = event.right().getValue().getString("id");
								if (classId != null && !classId.trim().isEmpty() &&
										request.params().contains("setDefaultRoles") &&
										config.getBoolean("classDefaultRoles", false)) {
									ApplicationUtils.setDefaultClassRoles(eb, classId, null);
								}
								renderJson(request, event.right().getValue(), 201);
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
		neo.send("MATCH (n:School)<-[:APPARTIENT]-(m:Class) " +
				"WHERE n.id = {id} " +
				"RETURN distinct m.name as name, m.id as classId, n.id as schoolId",
				params, request.response());
	}

	@SecuredAction("directory.authent")
	public void people(HttpServerRequest request) {
		List<String> expectedTypes = request.params().getAll("type");
		Map<String, Object> params = new HashMap<>();
		params.put("classId",request.params().get("id"));
		String types = "";
		if (expectedTypes != null && !expectedTypes.isEmpty()) {
			types = "AND (m:" + Joiner.on(" OR m:").join(expectedTypes) + ") ";
		}
		neo.send("MATCH (n:Class)<-[:APPARTIENT]-u-[:EN_RELATION_AVEC*0..1]->(m:User) "
				+ "WHERE n.id = {classId} " + types
				+ "RETURN distinct m.id as userId, HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User')) as type, "
				+ "m.activationCode as code, m.firstName as firstName,"
				+ "m.lastName as lastName, n.id as classId "
				+ "ORDER BY type DESC ", params, request.response());
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
				String firstname = request.formAttributes().get("firstname");
				String lastname = request.formAttributes().get("lastname");
				String type = request.formAttributes().get("type");
				List<String> childrenIds = request.formAttributes().getAll("childrenIds");
				if (classId != null && !classId.trim().isEmpty() &&
						firstname != null && !firstname.trim().isEmpty() &&
						lastname != null && !lastname.trim().isEmpty() &&
						type != null && !type.trim().isEmpty()) {
					final String userId = UUID.randomUUID().toString();
					final JsonObject user = new JsonObject()
							.putString("id", userId)
							.putString(ENTEleveCycle, "")
							.putString(ENTEleveNiveau, "")
							.putString(ENTPersonAdresse, "")
							.putString(ENTPersonCivilite, "")
							.putString(ENTPersonCodePostal, "")
							.putString(ENTPersonMail, "")
							.putString(ENTPersonNomPatro, "")
							.putString(ENTPersonPays, "")
							.putString(ENTPersonTelPerso, "")
							.putString(ENTPersonVille, "")
							.putString(ENTPersRelEleveTelMobile, "")
							.putString(ENTPersonClasses, classId)
							.putString(ENTPersonNom, lastname)
							.putString(ENTPersonPrenom, firstname)
							.putString(ENTPersonIdentifiant, idGenerator.generate())
							.putString(ENTPersonLogin, loginGenerator.generate(firstname, lastname))
							.putString(ENTPersonNomAffichage, displayNameGenerator.generate(firstname, lastname))
							.putString("activationCode", activationGenerator.generate());
					UserQueriesBuilder uqb = new UserQueriesBuilder();
					uqb.createUser(user, type)
							.linkClass(userId, classId)
							.linkSchool(userId, classId);
					if ("Relative".equals(type) && childrenIds != null && !childrenIds.isEmpty()) {
						uqb.linkChildrens(userId, childrenIds);
					}
					uqb.linkGroupProfils(userId, type);
					neo.sendBatch(uqb.build(), new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
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
								JsonArray a = new JsonArray().addString(userId);
								ApplicationUtils.publishModifiedUserGroup(eb, a);
								renderJson(request, res.body());
							} else {
								renderError(request, res.body());
							}
						}
					});
				} else {
					badRequest(request);
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
					"MATCH (m:User)-[:APPARTIENT]->g-[:DEPENDS]->n " +
					"WHERE (n:School OR n:Class) AND n.id = {id} AND NOT(m.activationCode IS NULL) " +
					"RETURN distinct m.lastName as lastName, m.firstName as firstName, " +
					"m.login as login, m.activationCode as activationCode, " +
					"HEAD(filter(x IN labels(m) WHERE x <> 'Visible' AND x <> 'User')) as type " +
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
		neo.send("MATCH (n:SuperAdmin) "
			+ "WHERE n.id = '" + admin.getString("id") + "' "
			+ "WITH count(*) AS exists "
			+ "WHERE exists=0 "
			+ "CREATE (m:SuperAdmin:User {id:'" + admin.getString("id") + "', "
			+ "lastName:'"+ admin.getString("firstname") +"', "
			+ "firstName:'"+ admin.getString("lastname") +"', "
			+ "login:'"+ admin.getString("login") +"', "
			+ "displayName:'"+ admin.getString("firstname") +" " + admin.getString("lastname") +"', "
			+ "password:'"+ BCrypt.hashpw(admin.getString("password"), BCrypt.gensalt()) +"'})");
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