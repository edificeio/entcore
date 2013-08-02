package edu.one.core.directory;

import static edu.one.core.infra.http.Renders.*;
import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.directory.profils.DefaultProfils;
import edu.one.core.directory.profils.Profils;
import edu.one.core.infra.Server;
import edu.one.core.infra.Neo;
import edu.one.core.infra.http.Renders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Directory extends Server {
	
	JsonObject admin;
	Neo neo;
	Dictionary d;

	public Directory() {
	}
	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(container);
		neo = new Neo(vertx.eventBus(),log);
		d = new DefaultDictionary(vertx, container, "../edu.one.core~dataDictionary~0.1.0-SNAPSHOT/aaf-dictionary.json");
		admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());
		final Profils p = new DefaultProfils(neo);

		neo.send("START n=node:node_auto_index(id='" + admin.getString("id") + "') "
			+ "WITH count(*) AS exists "
			+ "WHERE exists=0 "
			+ "CREATE (m {id:'" + admin.getString("id") + "', "
			+ "type:'" + admin.getString("type") + "',"
			+ "ENTPersonNom:'"+ admin.getString("firstname") +"', "
			+ "ENTPersonPrenom:'"+ admin.getString("lastname") +"', "
			+ "ENTPersonMotDePasse:'"+ admin.getString("password") +"'})");

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request, new JsonObject());
			}
		});

		rm.get("/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("type","ETABEDUCNAT");
				neo.send("START n=node:node_auto_index(type={type}) RETURN distinct n.ENTStructureNomCourant as name, n.id as id", params, request.response());
			}
		});

		rm.get("/api/groupes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("type","GROUPE");
				neo.send("START n=node:node_auto_index(type={type}) RETURN distinct n.ENTGroupeNom as name, n.id as id, n.ENTPeople as people", params, request.response());
			}
		});

		rm.get("/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("id",request.params().get("id"));
				params.put("type","CLASSE");
				neo.send("START n=node:node_auto_index(id={id}), m=node:node_auto_index(type={type}) MATCH n<--m RETURN distinct m.ENTGroupeNom as name, m.id as classId, n.id as schoolId", params, request.response());
			}
		});

		rm.get("/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				neo.send("START n=node(*) , m=node(*) MATCH n<--m WHERE has(m.type) AND has(n.id) AND n.id='"
						+ request.params().get("id") +"'"
						+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
						+ "RETURN distinct m.id as userId,m.ENTPersonNom as firstName,"
						+ "m.ENTPersonPrenom as lastName, n.id as classId", request.response());
			}
		});

		rm.get("/api/membres", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String[] people = request.params().get("data").replaceAll("\\[","").replaceAll("\\]","").split(", ");
				String requestIds = "n.id='" + people[0] + "'";
				for (int i = 1; i < people.length; i++) {
					requestIds += " OR n.id='" + people[i] + "'";
				}
				neo.send("START n=node(*) WHERE has(n.id) AND (" + requestIds + ") "
						+ "RETURN distinct n.id as id, n.ENTPersonNom as lastName, "
						+ "n.ENTPersonPrenom as firstName", request.response());
			}
		});

		rm.get("/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("id",request.params().get("id"));
				neo.send("START n=node:node_auto_index(id={id}) RETURN distinct n.ENTPersonNom as lastName, n.ENTPersonPrenom as firstName, n.ENTPersonAdresse as address", params, request.response());
			}
		});

		rm.get("/api/enseignants", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("id",request.params().get("id"));
				params.put("type","PERSEDUCNAT");
				neo.send("START n=node:node_auto_index(id={id}), m=node:node_auto_index(type={type}) RETURN distinct m.id as userId, m.ENTPersonNom as lastName, m.ENTPersonPrenom as firstName, n.id as classId", params, request.response());
			}
		});

		rm.get("/api/link", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				Map<String,Object> params = new HashMap<String,Object>();
				params.put("classId",request.params().get("class"));
				params.put("userId",request.params().get("id"));
				neo.send("START n=node:node_auto_index(id={classId}), m=node:node_auto_index(id={userId}) "
						+ "CREATE m-[:APPARTIENT]->n", request.response());
			}
		});

		rm.get("/api/create-user", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject obj = new JsonObject();
				Map<String,Boolean> params = d.validateFields(request.params());
				if (!params.values().contains(false)){
					trace.info("Creating new User : " + request.params().get("ENTPersonNom") + " " + request.params().get("ENTPersonPrenom"));
					obj.putString("id", request.params().get("ENTPersonIdentifiant"))
							.putString("nom", request.params().get("ENTPersonNom"))
							.putString("prenom", request.params().get("ENTPersonPrenom"))
							.putString("login", request.params().get("ENTPersonNom") + "." + request.params().get("ENTPersonPrenom"))
							.putString("classe", "4400000002_ORDINAIRE_CM2deMmeRousseau")
							.putString("type", request.params().get("ENTPersonProfils"))
							.putString("password", "dummypass");
					vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
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
		});

		rm.get("/api/create-admin", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
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
		});

		rm.get("/api/create-group", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				List users = new ArrayList<String>();
				for (Map.Entry<String, String> entry : request.params()) {
					if (!entry.getKey().startsWith("ENT") && !entry.getKey().equals("type")){
						users.add(entry.getValue());
					}
				}
				JsonObject obj = new JsonObject().putString("id", request.params().get("ENTGroupId"))
						.putString("nom", request.params().get("ENTGroupName"))
						.putString("parent", "4400000002_ORDINAIRE_CM2deMmeRousseau")
						.putString("type", request.params().get("type"));
				vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
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
				trace.info("Creating new Group : " + request.params().get("ENTGroupName"));
			}
		});
		
		rm.get("/api/create-school", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject obj = new JsonObject().putString("id", request.params().get("ENTSchoolId"))
						.putString("nom", request.params().get("ENTSchoolName"))
						.putString("type", "ETABEDUCNAT");
				vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
					public void handle(Message event) {
						container.logger().info("MESSAGE : " + event.body());
					}
				});
				neo.send("START n=node(*) "
						+ "CREATE (m {id:'" + request.params().get("ENTSchoolId")
						+ "', type:'ETABEDUCNAT',"
						+ "ENTStructureNomCourant:'" + request.params().get("ENTSchoolName")
						+ "'})", request.response());
				trace.info("Creating new School : " + request.params().get("ENTSchoolName"));
			}
		});

		rm.get("/api/export", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = "";
				if (request.params().get("id").equals("all")){
					neoRequest = "START m=node(*) WHERE has(m.type) "
							+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
							+ "RETURN distinct m.id,m.ENTPersonNom as lastName, m.ENTPersonPrenom as firstName, "
							+ "m.ENTPersonLogin as login, m.ENTPersonMotDePasse as password";
				} else {
					neoRequest = "START n=node(*), m=node(*) MATCH n<--m WHERE has(n.id) AND n.id='" + request.params().get("id") + "' "
							+ "AND has(m.type) AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
							+ "RETURN distinct m.id,m.ENTPersonNom as lastName,"
							+ "m.ENTPersonPrenom as firstName, m.ENTPersonLogin as login, "
							+ "m.ENTPersonMotDePasse as password";
				}
				trace.info("Exporting auth data for " + request.params().get("id"));
				neo.send(neoRequest, request.response());
			}
		});

		rm.post("/api/group-profil", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest request) {
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
		});

		vertx.eventBus().registerHandler("directory", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				String action = message.body().getString("action");
				switch (action) {
				case "groups":
					p.listGroupsProfils(new Handler<JsonObject>() {
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
		});

	}
}