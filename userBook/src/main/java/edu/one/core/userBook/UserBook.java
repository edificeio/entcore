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
				//TODO : check if current user has userbook node
				if (request.params().contains("id")){//simulate user id in url
					neo.send("START n=node(*) WHERE has(n.ENTPersonIdentifiant) AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
					+ "CREATE (m {userId:'" + request.params().get("id") + "', "
					+ "picture:'" + userBookData.getString("picture") + "',"
					+ "motto:'', health:'', mood:'default'}), n-[:USERBOOK]->m ");
					for (Object hobby : hobbies) {
						JsonObject jo = (JsonObject)hobby;
						neo.send("START n=node(*) WHERE has(n.userId) AND n.userId='" + request.params().get("id") + "' "
						+ "CREATE (m {category:'" + jo.getString("code") + "'}), "
						+ "n-[:PUBLIC]->m ");
					}
				}
				renderView(request, new JsonObject());
			}
		});

		rm.get("/annuaire", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject jo = new JsonObject();
				if (request.params().contains("query")){
					jo = new JsonObject(request.params().get("query"));
				}
				renderView(request, jo);
			}
		});

		rm.get("/api/search", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("name")){
					neo.send("START n=node(*),m=node(*) MATCH n-[USERBOOK]->m "
						+ "WHERE has(n.ENTPersonNomAffichage) "
						+ "AND n.ENTPersonNomAffichage='" + request.params().get("name") + "' "
						+ "AND has(m.motto) RETURN distinct n.ENTPersonIdentifiant as id, "
						+ "n.ENTPersonNomAffichage as displayName, m.mood as mood;",request.response());
				} else if (request.params().contains("class")){
					neo.send("START n=node(*),m=node(*) MATCH n<-[APPARTIENT]-m WHERE has(m.type) "
						+ "AND has(n.ENTGroupeNom) AND n.ENTGroupeNom='" + request.params().get("class") + "' "
						+ "RETURN m.ENTPersonIdentifiant as id,m.ENTPersonNomAffichage as displayName"
						, request.response());
				}
			}
		});
		rm.get("/api/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("id")){
					neo.send("START n=node(*),m=node(*) MATCH n-[USERBOOK]->m "
						+ "WHERE has(n.ENTPersonIdentifiant) "
						+ "AND n.ENTPersonIdentifiant='" + request.params().get("id") + "' "
						+ "AND has(m.motto) RETURN distinct n.ENTPersonNomAffichage as displayName, "
						+ "n.ENTPersonAdresse as address, m.motto as motto, "
						+ "m.mood as mood, m.health as health;",request.response());
				}
			}
		});
	}
}