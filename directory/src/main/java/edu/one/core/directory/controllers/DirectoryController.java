package edu.one.core.directory.controllers;

import static edu.one.core.directory.be1d.BE1DConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import com.google.common.base.Joiner;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.datadictionary.generation.ActivationCodeGenerator;
import edu.one.core.datadictionary.generation.DisplayNameGenerator;
import edu.one.core.datadictionary.generation.IdGenerator;
import edu.one.core.datadictionary.generation.LoginGenerator;
import edu.one.core.directory.be1d.BE1D;
import edu.one.core.directory.profils.DefaultProfils;
import edu.one.core.directory.profils.Profils;
import edu.one.core.directory.users.UserQueriesBuilder;
import edu.one.core.infra.Controller;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.security.BCrypt;
import edu.one.core.security.SecuredAction;


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

	public DirectoryController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.neo = new Neo(eb,log);
			this.config = config;
			this.d = new DefaultDictionary(vertx, container, "aaf-dictionary.json");
			this.admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
			this.p = new DefaultProfils(neo);
			loginGenerator = new LoginGenerator();
			activationGenerator = new ActivationCodeGenerator();
			idGenerator = new IdGenerator();
			displayNameGenerator = new DisplayNameGenerator();
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
		neo.send("MATCH (n:Class)<-[:APPARTIENT]-(m:User) "
				+ "WHERE n.id = {classId} " + types
				+ "RETURN distinct m.id as userId, HEAD(filter(x IN labels(m) WHERE x <> 'User')) as type, "
				+ "m.activationCode as code, m.firstName as firstName,"
				+ "m.lastName as lastName, n.id as classId", params, request.response());
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
				String classId = request.formAttributes().get("classId");
				String firstname = request.formAttributes().get("firstname");
				String lastname = request.formAttributes().get("lastname");
				String type = request.formAttributes().get("type");
				List<String> childrenIds = request.formAttributes().getAll("childrenIds");
				if (classId != null && !classId.trim().isEmpty() &&
						firstname != null && !firstname.trim().isEmpty() &&
						lastname != null && !lastname.trim().isEmpty() &&
						type != null && !type.trim().isEmpty()) {
					String userId = UUID.randomUUID().toString();
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
					uqb.linkGroupProfils(userId, type)
							.defaultCommunication(userId, type);
					neo.sendBatch(uqb.build(), new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
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
					+ "HEAD(filter(x IN labels(m) WHERE x <> 'User')) as type "
					+ "ORDER BY type, login ";
		} else if (request.params().get("id") != null) {
			neoRequest =
					"MATCH (m:User)-[:APPARTIENT]->g-[:DEPENDS]->n " +
					"WHERE (n:School OR n:Class) AND n.id = {id} AND NOT(m.activationCode IS NULL) " +
					"RETURN distinct m.ENTPersonNom as lastName, m.ENTPersonPrenom as firstName, " +
					"m.ENTPersonLogin as login, m.activationCode as activationCode, m.type as type " +
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
					String export;
					try {
						export = processTemplate(request, "text/export.txt",
								new JsonObject().putArray("list", r));
						request.response().putHeader("Content-Type", "application/csv");
						request.response().putHeader("Content-Disposition",
								"attachment; filename=activation_de_comptes.csv");
						request.response().end(export);
					} catch (IOException e) {
						renderError(request);
					}
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