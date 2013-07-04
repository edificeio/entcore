package edu.one.core.userBook;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class UserBook extends Controller {

	Neo neo;
	JsonObject users;

	@Override
	public void start() {
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		final JsonObject userBookData= new JsonObject(vertx.fileSystem().readFileSync("userBook-data.json").toString());
		final JsonArray hobbies = userBookData.getArray("hobbies");

		rm.get("/mon-compte", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("id")){
					neo.send("START n=node(*) WHERE has(n.ENTPersonIdentifiant) "
						+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
						+ "CREATE UNIQUE (n)-[:USERBOOK]->(m {picture:'" + userBookData.getString("picture") + "',"
						+ "motto:'', health:'', mood:'default'})");
					for (Object hobby : hobbies) {
						JsonObject jo = (JsonObject)hobby;
						neo.send("START n=node(*),m=node(*) WHERE has(n.ENTPersonIdentifiant) "
							+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
							+ "CREATE UNIQUE (n)-[:USERBOOK]->(m)-[:PUBLIC]->(p {category:'"
							+ jo.getString("code") + "', value:''})");
					}
				}
				renderView(request, new JsonObject());
			}
		});

		rm.get("/annuaire", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

		rm.get("/api/search", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = "START n=node(*) ";
				if (request.params().contains("name")){
					neoRequest += ", m=node(*) MATCH n-[USERBOOK]->m WHERE has(n.ENTPersonNomAffichage) "
						+ "AND n.ENTPersonNomAffichage=~'" + request.params().get("name").substring(0,3) + ".*' AND has(m.motto)";
				} else if (request.params().contains("class")){
					neoRequest += "m=node(*) MATCH m<-[APPARTIENT]-n WHERE has(n.type) "
						+ "AND has(n.ENTGroupeNom) AND n.ENTGroupeNom='" + request.params().get("class") + "'";
				}
				neoRequest += " RETURN distinct n.ENTPersonIdentifiant as id, "
					+ "n.ENTPersonNomAffichage as displayName, m.mood as mood";
				neo.send(neoRequest, request.response());
			}
		});
		rm.get("/api/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("id")){
					neo.send("START n=node(*),m=node(*), p=node(*) MATCH n-[r]->m, m-[s]->p "
						+ "WHERE has(n.ENTPersonIdentifiant) AND type(r)<>'APPARTIENT' AND type(s)<>'APPARTIENT' "
						+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
						+ "RETURN n.ENTPersonNomAffichage as displayName, "
						+ "n.ENTPersonAdresse as address, m.motto? as motto, "
						+ "m.mood? as mood, m.health? as health, p.category? as category, p.value? as value;"
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
	}
}