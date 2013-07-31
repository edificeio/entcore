package edu.one.core.userBook;

import edu.one.core.infra.Server;
import edu.one.core.infra.Neo;
import edu.one.core.infra.http.Renders;

import org.vertx.java.core.Handler;
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

		rm.get("/mon-compte", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("init")){
					neo.send("START n=node(*) WHERE has(n.ENTPersonNomAffichage) "
						+ "AND n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
						+ "CREATE (m {userId:'" + request.params().get("init") + "', "
						+ "picture:'" + userBookData.getString("picture") + "',"
						+ "motto:'Ajoute ta devise', health:'Problèmes de santé ?', mood:'default'}), n-[:USERBOOK]->m ");
					for (Object hobby : hobbies) {
						JsonObject jo = (JsonObject)hobby;
						neo.send("START n=node(*),m=node(*) MATCH n-[r]->m WHERE has(n.ENTPersonNomAffichage) "
							+ "AND n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
							+ "AND type(r)='USERBOOK' CREATE (p {category:'" + jo.getString("code") 
							+ "', values:'testval, othertestval'}), m-[:PUBLIC]->p");
					}
					neo.send("START n=node(*) WHERE has(n.ENTPersonNomAffichage) "
							+ "AND n.ENTPersonNomAffichage='" + request.params().get("init") + "' "
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
				String neoRequest = "START n=node(*) ";
				if (request.params().contains("name")){
					neoRequest += " MATCH (n)-[r*]->(m) WHERE has(n.ENTPersonNomAffichage) "
						+ "AND n.ENTPersonNomAffichage=~'" + request.params().get("name").substring(0,3) + ".*'";
				} else if (request.params().contains("class")){
					neoRequest += "m=node(*) MATCH m<-[APPARTIENT]-n WHERE has(n.type) "
						+ "AND has(n.ENTGroupeNom) AND n.ENTGroupeNom='" + request.params().get("class") + "'";
				}
				neoRequest += " RETURN distinct n.ENTPersonIdentifiant as id, "
					+ "n.ENTPersonNomAffichage as displayName, m.mood? as mood";
				neo.send(neoRequest, request.response());
			}
		});
		rm.get("/api/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("id")){
					neo.send("START n=node(*) MATCH (n)-[r*]->(m) "
						+ "WHERE has(n.ENTPersonIdentifiant) "
						+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
						+ "RETURN distinct n.ENTPersonNomAffichage as displayName, "
						+ "n.ENTPersonIdentifiant as id, "
						+ "n.ENTPersonAdresse as address, m.motto? as motto, "
						+ "m.mood? as mood, m.health? as health, m.category? as category, "
						+ "m.values? as values, EXTRACT(rel in r: type(rel)) as relation;"
						,request.response());
				}
			}
		});
		rm.get("/api/class", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("name")){
					neo.send("START n=node(*),m=node(*) MATCH n<-[APPARTIENT]-m WHERE has(m.type) "
						+ "AND has(n.ENTGroupeNom) AND n.ENTGroupeNom='" + request.params().get("name") + "' "
						+ "RETURN m.ENTPersonIdentifiant as id,m.ENTPersonNomAffichage as displayName"
						, request.response());
				}
			}
		});

		rm.get("/api/edit-userbook-info", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = "START n=node(*) MATCH (n)-[USERBOOK]->(m)";
				if (request.params().contains("category")){
					neoRequest += ", (m)-->(p) WHERE has(n.ENTPersonIdentifiant) "
					+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' AND has(p.category) "
					+ "AND p.category='" + request.params().get("category") + "' "
					+ "SET p.values='" + request.params().get("values") + "'";
				} else {
					neoRequest += " WHERE has(n.ENTPersonIdentifiant) "
					+ "AND n.ENTPersonIdentifiant='" + request.params().get("id")
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
	}
}