package edu.one.core.userBook;

import edu.one.core.infra.Server;
import edu.one.core.infra.Neo;
import edu.one.core.infra.http.HttpClientUtils;
import edu.one.core.infra.http.Renders;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class UserBook extends Server {

	Neo neo;
	JsonObject userBookData;

	@Override
	public void start() {
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		final Renders render = new Renders(container);
		userBookData= new JsonObject(vertx.fileSystem().readFileSync("userBook-data.json").toString());
		final JsonArray hobbies = userBookData.getArray("hobbies");
		final String workspaceUrl = config.getString("workspace-url", "http://localhost:8011");
		final int port = config.getInteger("workspace-port", 8011);
		final HttpClient client = vertx.createHttpClient().setHost("localhost").setPort(port);

		rm.get("/mon-compte", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("init")){
					neo.send("START n=node:node_auto_index(type='ELEVE') WHERE "
						+ "n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
						+ "CREATE (m {userId:'" + request.params().get("init") + "', "
						+ "picture:'" + userBookData.getString("picture") + "',"
						+ "motto:'Ajoute ta devise', health:'Problèmes de santé ?', mood:'default'}), n-[:USERBOOK]->m ");
					for (Object hobby : hobbies) {
						JsonObject jo = (JsonObject)hobby;
						neo.send("START n=node:node_auto_index(type='ELEVE'),m=node(*) MATCH n-[r]->m WHERE "
							+ "n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
							+ "AND type(r)='USERBOOK' CREATE (p {category:'" + jo.getString("code") 
							+ "', values:'testval, othertestval'}), m-[:PUBLIC]->p");
					}
					neo.send("START n=node:node_auto_index(type='ELEVE') WHERE "
							+ "n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
							+ "RETURN n.ENTPersonIdentifiant",request.response());
				} else if (request.params().contains("id")){
					render.renderView(request, userBookData);
				}
			}
		});

		rm.get("/annuaire", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request);
			}
		});

		rm.get("/api/search", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = "START n=node:node_auto_index(type='ELEVE') ";
				if (request.params().contains("name")){
					String[] names = request.params().get("name").split(" ");
					String displayNameRegex = (names[0].length() > 3) ? "(?i)(.*" + names[0].substring(0,4) : "(?i)(.*" + names[0];
					for (int i = 1; i < names.length; i++) {
						displayNameRegex += (names[i].length() > 3) ? ".*|.*" + names[i].substring(0,4) : ".*|.*" + names[i];
					}
					displayNameRegex += ".*)";
					neoRequest += " MATCH (n)-[USERBOOK]->(m) WHERE n.ENTPersonNomAffichage=~'" + displayNameRegex + "'";
				} else if (request.params().contains("class")){
					neoRequest += ", m=node:node_auto_index(type='CLASSE') MATCH m<-[APPARTIENT]-n WHERE "
						+ " m.ENTGroupeNom='" + request.params().get("class") + "'";
				}
				neoRequest += " RETURN distinct n.ENTPersonIdentifiant as id, "
					+ "n.ENTPersonNomAffichage as displayName, m.mood? as mood, n.type as type";
				neo.send(neoRequest, request.response());
			}
		});
		rm.get("/api/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("id") && request.params().contains("type")){
					String personRequest = "";
					String personRequestStart = "START n=node:node_auto_index(type='" + request.params().get("type") + "')";
					String personRequestReturn= ",(n)-[USERBOOK]->(u),(u)-[r]->(c) WHERE has(n.ENTPersonIdentifiant) "
							+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
							+ "AND has(m.ENTPersonLogin) RETURN distinct n.ENTPersonNomAffichage as displayName, "
							+ "n.ENTPersonIdentifiant as id,n.ENTPersonAdresse as address, m.ENTPersonNomAffichage as relatedName, "
							+ "m.ENTPersonIdentifiant as relatedId,m.type as relatedType,u.motto? as motto, u.picture? as photo, u.mood? as mood, "
							+ "u.health? as health, c.category? as category, c.values? as values;";

					switch(request.params().get("type")){
						case "ELEVE":
							personRequest = personRequestStart + ",m=node:node_auto_index(type='PERSRELELEVE') "
								+ "MATCH (n)-[EN_RELATION_AVEC]->(m)" + personRequestReturn;
							break;
						case "ENSEIGNANT":
							personRequest = personRequestStart + "MATCH (n)-[USERBOOK]->(u),(u)-[r]->(c) "
								+ "WHERE has(n.ENTPersonIdentifiant) AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
								+ "RETURN distinct n.ENTPersonNomAffichage as displayName, n.ENTPersonIdentifiant as id, "
								+ "n.ENTPersonAdresse as address,u.motto? as motto, u.picture? as photo, u.mood? as mood, "
								+ "u.health? as health, c.category? as category, c.values? as values;";
							break;
						case "PERSRELELEVE":
							personRequest = personRequestStart + ",m=node:node_auto_index(type='ELEVE') "
								+ "MATCH (n)<-[EN_RELATION_AVEC]-(m)" + personRequestReturn;
							break;

					}
					neo.send(personRequest,request.response());
				}
			}
		});
		rm.get("/api/class", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("name")){
					neo.send("START n=node:node_auto_index(type='CLASSE') MATCH (n)<-[APPARTIENT]-(m) WHERE HAS(n.ENTGroupeNom) AND"
						+ " n.ENTGroupeNom='" + request.params().get("name") + "' AND (m.type='ENSEIGNANT' OR m.type='ELEVE') RETURN"
						+ " m.type as type, m.ENTPersonIdentifiant as id,m.ENTPersonNomAffichage as displayName"
						, request.response());
				}
			}
		});

		rm.get("/api/edit-userbook-info", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = "START n=node:node_auto_index(type='ELEVE') MATCH (n)-[USERBOOK]->(m)";
				if (request.params().contains("category")){
					neoRequest += ", (m)-->(p) WHERE n.ENTPersonIdentifiant='" + request.params().get("id") + "' AND has(p.category) "
					+ "AND p.category='" + request.params().get("category") + "' "
					+ "SET p.values='" + request.params().get("values") + "'";
				} else {
					neoRequest += " WHERE n.ENTPersonIdentifiant='" + request.params().get("id")
					+ "' SET m." + request.params().get("prop") + "='" + request.params().get("value") + "'";
				}
				neo.send(neoRequest, request.response());
			}
		});

		rm.get("/api/set-visibility", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START r=relationship(*) MATCH (n)-[r]->(m),(m)-[s]->(p) "
					+ "WHERE type(r)='USERBOOK' AND HAS (n.ENTPersonIdentifiant) AND n.ENTPersonIdentifiant='"
					+ request.params().get("id") + "' AND p.category='"+ request.params().get("category")
					+ "' DELETE s CREATE (m)-[j:"+ request.params().get("value") +"]->(p) "
					+ "RETURN n,r,m,j,p", request.response());
			}
		});

		rm.allWithRegEx(".*", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				HttpClientUtils.proxy(request, client);
			}
		});
	}
}