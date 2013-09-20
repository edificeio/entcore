package edu.one.core.directory.controllers;

import static edu.one.core.directory.be1d.BE1DConstants.*;

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
import edu.one.core.directory.be1d.WordpressHelper;
import edu.one.core.directory.profils.DefaultProfils;
import edu.one.core.directory.profils.Profils;
import edu.one.core.directory.users.UserQueriesBuilder;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
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
			this.d = new DefaultDictionary(vertx, container, "../edu.one.core~dataDictionary~1.0.0/aaf-dictionary.json");
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
		Map<String, Object> params = new HashMap<>();
		params.put("type","ETABEDUCNAT");
		neo.send("START n=node:node_auto_index(type={type}) RETURN distinct n.ENTStructureNomCourant as name, n.id as id", params, request.response());
	}

	@SecuredAction("directory.classes")
	public void classes(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		neo.send("START n=node:node_auto_index(id={id}) " +
				"MATCH n<-[:APPARTIENT]-m " +
				"WHERE has(m.type) AND m.type = 'CLASSE' " +
				"RETURN distinct m.ENTGroupeNom as name, m.id as classId, n.id as schoolId",
				params, request.response());
	}

	@SecuredAction("directory.authent")
	public void people(HttpServerRequest request) {
		List<String> expectedTypes = request.params().getAll("type");
		Map<String, Object> params = new HashMap<>();
		params.put("classId",request.params().get("id"));
		String types = "'ELEVE','ENSEIGNANT','PERSRELELEVE'";
		if (expectedTypes != null && !expectedTypes.isEmpty()) {
			types = "'" + Joiner.on(',').join(expectedTypes) + "'";
		}
		neo.send("START n=node:node_auto_index(id={classId}) "
				+ "MATCH n<-[:APPARTIENT]-m "
				+ "WHERE m.type IN [" + types + "] "
				+ "RETURN distinct m.id as userId, m.type as type,  m.activationCode? as code, m.ENTPersonNom as firstName,"
				+ "m.ENTPersonPrenom as lastName, n.id as classId", params, request.response());
	}

	@SecuredAction("directory.authent")
	public void details(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		neo.send("START n=node:node_auto_index(id={id}) RETURN distinct "
				+ "n.ENTPersonLogin as login, n.ENTPersonAdresse as address, "
				+ "n.activationCode? as code;"
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
				List<String> childrenIds =  request.formAttributes().getAll("childrenIds");
				if (classId != null && !classId.trim().isEmpty() &&
						firstname != null && !firstname.trim().isEmpty() &&
						lastname != null && !lastname.trim().isEmpty() &&
						type != null && !type.trim().isEmpty()) {
					String userId = UUID.randomUUID().toString();
					final JsonObject user = new JsonObject()
					.putString("id", userId)
					.putString("type", type)
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
					uqb.createUser(user)
					.linkClass(userId, classId);
					if ("PERSRELELEVE".equals(type) && childrenIds != null && !childrenIds.isEmpty()) {
						uqb.linkChildrens(userId, childrenIds);
					}
					uqb.linkGroupProfils(userId, type)
					.defaultCommunication(userId, type);
					neo.sendBatch(uqb.build(), new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> res) {
							if ("ok".equals(res.body().getString("status"))) {
								WordpressHelper.sendUser(eb, user);
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
	public void export(HttpServerRequest request) {
		String neoRequest = "";
		if (request.params().get("id").equals("all")){ // TODO filter by school
			neoRequest = "START m=node:node_auto_index(" +
					"'type:ELEVE OR type:PERSEDUCNAT OR type:PERSRELELEVE OR type:ENSEIGNANT') " +
					"WHERE has(m.activationCode) "
					+ "RETURN distinct m.id,m.ENTPersonNom as lastName, m.ENTPersonPrenom as firstName, "
					+ "m.ENTPersonLogin as login, m.activationCode as activationCode";
		} else {
			neoRequest = "START m=node:node_auto_index(id='" + request.params().get("id") + "') "
					+ "WHERE has(m.activationCode) AND has(m.type) AND (m.type='ELEVE' "
					+ "OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE' OR m.type='ENSEIGNANT') "
					+ "RETURN distinct m.id,m.ENTPersonNom as lastName,"
					+ "m.ENTPersonPrenom as firstName, m.ENTPersonLogin as login, "
					+ "m.activationCode as activationCode";
		}
		neo.send(neoRequest, request.response());
	}

	@SecuredAction("directory.authent")
	public void groupProfile(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				String profil = request.formAttributes().get("profil");
				if (profil != null && !profil.trim().isEmpty()) {
					p.createGroupProfil(profil, new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject res) {
							if ("ok".equals(res.getString("status"))) {
								renderJson(request, res);
							} else {
								renderError(request, res);
							}
						}
					});
				} else {
					badRequest(request);
				}
			}
		});
	}

	public void createSuperAdmin(){
		neo.send("START n=node:node_auto_index(id='" + admin.getString("id") + "') "
			+ "WITH count(*) AS exists "
			+ "WHERE exists=0 "
			+ "CREATE (m {id:'" + admin.getString("id") + "', "
			+ "type:'" + admin.getString("type") + "',"
			+ "ENTPersonNom:'"+ admin.getString("firstname") +"', "
			+ "ENTPersonPrenom:'"+ admin.getString("lastname") +"', "
			+ "ENTPersonLogin:'"+ admin.getString("login") +"', "
			+ "ENTPersonNomAffichage:'"+ admin.getString("firstname") +" " + admin.getString("lastname") +"', "
			+ "ENTPersonMotDePasse:'"+ BCrypt.hashpw(admin.getString("password"), BCrypt.gensalt()) +"'})");
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