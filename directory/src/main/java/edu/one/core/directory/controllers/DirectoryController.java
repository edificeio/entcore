package edu.one.core.directory.controllers;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.directory.be1d.BE1D;
import edu.one.core.directory.profils.DefaultProfils;
import edu.one.core.directory.profils.Profils;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import edu.one.core.infra.Server;
import edu.one.core.infra.security.BCrypt;
import edu.one.core.security.SecuredAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;


public class DirectoryController extends Controller {

	private Neo neo;
	private JsonObject config;
	private JsonObject admin;
	private Dictionary d;
	private Profils p;
	protected final EventBus eb;

	public DirectoryController(Vertx vertx, Container container,
		RouteMatcher rm, Map<String, edu.one.core.infra.security.SecuredAction> securedActions, JsonObject config) {
			super(vertx, container, rm, securedActions);
			this.eb = Server.getEventBus(vertx);
			this.neo = new Neo(eb,log);
			this.config = config;
			this.d = new DefaultDictionary(vertx, container, "../edu.one.core~dataDictionary~0.1.0-SNAPSHOT/aaf-dictionary.json");
			this.admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
			this.p = new DefaultProfils(neo);
		}

	@SecuredAction("directory.authent")
	public void directory(HttpServerRequest request) {
		renderView(request, new JsonObject());
	}

	@SecuredAction("directory.authent")
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

	@SecuredAction("directory.authent")
	public void groups(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("type","GROUPE");
		neo.send("START n=node:node_auto_index(type={type}) RETURN distinct n.ENTGroupeNom as name, n.id as id, n.ENTPeople as people", params, request.response());
	}

	@SecuredAction("directory.authent")
	public void classes(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		params.put("type","CLASSE");
		neo.send("START n=node:node_auto_index(id={id}), m=node:node_auto_index(type={type}) MATCH n<--m RETURN distinct m.ENTGroupeNom as name, m.id as classId, n.id as schoolId", params, request.response());
	}

	@SecuredAction("directory.authent")
	public void people(HttpServerRequest request) {
		neo.send("START n=node(*) , m=node(*) MATCH n<--m WHERE has(m.type) AND has(n.id) AND n.id='"
				+ request.params().get("id") +"'"
				+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
				+ "RETURN distinct m.id as userId, m.activationCode? as code, m.ENTPersonNom as firstName,"
				+ "m.ENTPersonPrenom as lastName, n.id as classId", request.response());
	}

	@SecuredAction("directory.authent")
	public void members(HttpServerRequest request) {
		String[] people = request.params().get("data").replaceAll("\\[","").replaceAll("\\]","").split(", ");
		String requestIds = "n.id='" + people[0] + "'";
		for (int i = 1; i < people.length; i++) {
			requestIds += " OR n.id='" + people[i] + "'";
		}
		neo.send("START n=node(*) WHERE has(n.id) AND (" + requestIds + ") "
				+ "RETURN distinct n.id as id, n.ENTPersonNom as lastName, "
				+ "n.ENTPersonPrenom as firstName", request.response());
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

	@SecuredAction("directory.authent")
	public void teachers(HttpServerRequest request) {
		Map<String, Object> params = new HashMap<>();
		params.put("id",request.params().get("id"));
		params.put("type","PERSEDUCNAT");
		neo.send("START n=node:node_auto_index(id={id}), m=node:node_auto_index(type={type}) RETURN distinct m.id as userId, m.ENTPersonNom as lastName, m.ENTPersonPrenom as firstName, n.id as classId", params, request.response());
	}

	@SecuredAction("directory.authent")
	public void link(HttpServerRequest request) {
		Map<String,Object> params = new HashMap<>();
		params.put("classId",request.params().get("class"));
		params.put("userId",request.params().get("id"));
		neo.send("START n=node:node_auto_index(id={classId}), m=node:node_auto_index(id={userId}) "
				+ "CREATE m-[:APPARTIENT]->n", request.response());
	}

	@SecuredAction("directory.authent")
	public void createUser(HttpServerRequest request) {
		JsonObject obj = new JsonObject();
		Map<String,Boolean> params = d.validateFields(request.params());
		if (!params.values().contains(false)){
			obj.putString("id", request.params().get("ENTPersonIdentifiant"))
					.putString("nom", request.params().get("ENTPersonNom"))
					.putString("prenom", request.params().get("ENTPersonPrenom"))
					.putString("login", request.params().get("ENTPersonNom") + "." + request.params().get("ENTPersonPrenom"))
					.putString("classe", "4400000002_ORDINAIRE_CM2deMmeRousseau")
					.putString("type", request.params().get("ENTPersonProfils"))
					.putString("password", "dummypass");
			eb.send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
				public void handle(Message event) {
					container.logger().info("MESSAGE : " + event.body());
				}
			});
			neo.send("START n=node(*) WHERE has(n.ENTGroupeNom) "
					+ "AND n.ENTGroupeNom='" + request.params().get("ENTPersonStructRattach") + "' "
					+ "CREATE (m {id:'" + request.params().get("ENTPersonIdentifiant") + "', " 
					+ "type:'" + request.params().get("ENTPersonProfils") + "',"
					+ "ENTPersonNom:'"+request.params().get("ENTPersonNom") +"', "
					+ "ENTPersonPrenom:'"+request.params().get("ENTPersonPrenom") +"', "
					+ "ENTPersonDateNaissance:'"+request.params().get("ENTPersonDateNaissance") +"'}), "
					+ "m-[:APPARTIENT]->n ", request.response());
		} else {
			obj.putString("result", "error");
			for (Map.Entry<String, Boolean> entry : params.entrySet()) {
				if (!entry.getValue()){
					obj.putBoolean(entry.getKey(), entry.getValue());
				}
			}
			renderJson(request, obj);
		}
	}

	@SecuredAction("directory.authent")
	public void createAdmin(HttpServerRequest request) {
		String start = "";
		String conditions= "";
		String creation = "";
		for (Map.Entry<String, String> entry : request.params()) {
			if (!entry.getKey().startsWith("ENT")){
				start += entry.getKey()+"=node(*), ";
				conditions += "has("+entry.getKey()+".ENTStructureNomCourant) AND "
						+entry.getKey()+".ENTStructureNomCourant='"+entry.getValue() + "' AND ";
				creation += "m-[:ADMINISTRE]->" + entry.getKey() + ", ";
			}
		}
		if (request.params().get("ENTPerson").equals("none")){
			JsonObject obj = new JsonObject();
			//TODO : Send new user to WP (with multi groups attribute)
			neo.send("START " + start.substring(0,start.length()-2) + " WHERE "+ conditions.substring(0, conditions.length()-4)
				+ "CREATE (m {id:'" + request.params().get("ENTAdminId") + "', "
				+ "type:'CORRESPONDANT',"
				+ "ENTPersonNom:'"+request.params().get("ENTAdminNom") +"', "
				+ "ENTPersonPrenom:'"+request.params().get("ENTAdminPrenom") +"', "
				+ "ENTPersonDateNaissance:'"+request.params().get("ENTAdminBirthdate") +"'}), "
				+ creation.substring(0, creation.length()-2), request.response());
		} else {
			JsonObject obj = new JsonObject();
			//TODO : Send link user to groups to WP Connector
			neo.send("START m=node(*), "+ start.substring(0,start.length()-2) + " WHERE " +conditions
				+ "has(m.ENTPersonIdentifiant) AND m.ENTPersonIdentifiant='"
				+ request.params().get("ENTPerson") + "' CREATE "
				+ creation.substring(0, creation.length()-2), request.response());
		}
	}

	@SecuredAction("directory.authent")
	public void createGroup(HttpServerRequest request) {
		List users = new ArrayList<>();
		for (Map.Entry<String, String> entry : request.params()) {
			if (!entry.getKey().startsWith("ENT") && !entry.getKey().equals("type")){
				users.add(entry.getValue());
			}
		}
		JsonObject obj = new JsonObject().putString("id", request.params().get("ENTGroupId"))
				.putString("nom", request.params().get("ENTGroupName"))
				.putString("parent", "4400000002_ORDINAIRE_CM2deMmeRousseau")
				.putString("type", request.params().get("type"));
		eb.send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
			public void handle(Message event) {
				container.logger().info("MESSAGE : " + event.body());
			}
		});
		neo.send("START n=node(*) WHERE has(n.id) AND n.id='4400000002'"
				+ "CREATE (m {id:'"+request.params().get("ENTGroupId")+"',"
				+ "type:'"+request.params().get("type")+"',"
				+ "ENTGroupeNom:'"+request.params().get("ENTGroupName")
				+"', ENTPeople:'" + users.toString() + "'}), "
				+ "m-[:APPARTIENT]->n ", request.response());
	}

	@SecuredAction("directory.authent")
	public void createSchool(HttpServerRequest request) {
		JsonObject obj = new JsonObject().putString("id", request.params().get("ENTSchoolId"))
				.putString("nom", request.params().get("ENTSchoolName"))
				.putString("type", "ETABEDUCNAT");
		eb.send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
			public void handle(Message event) {
				container.logger().info("MESSAGE : " + event.body());
			}
		});
		neo.send("START n=node(*) "
				+ "CREATE (m {id:'" + request.params().get("ENTSchoolId")
				+ "', type:'ETABEDUCNAT',"
				+ "ENTStructureNomCourant:'" + request.params().get("ENTSchoolName")
				+ "'})", request.response());
	}

	@SecuredAction("directory.authent")
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

	@SecuredAction("directory.authent")
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


	@SecuredAction("directory.authent")
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