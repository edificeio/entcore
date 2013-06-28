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

		rm.get("/index", new Handler<HttpServerRequest>() {
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
				renderView(request);
			}
		});
		
		rm.get("/api", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if (request.params().contains("name")){
					neo.send("START n=node(*),m=node(*) MATCH n-[USERBOOK]->m "
						+ "WHERE has(n.ENTPersonNomAffichage) "
						+ "AND n.ENTPersonNomAffichage='" + request.params().get("name") + "' "
						+ "AND has(m.motto) RETURN distinct n.ENTPersonIdentifiant as id, "
						+ "n.ENTPersonNomAffichage as displayName, "
						+ "n.ENTPersonAdresse as address, m.motto as motto, "
						+ "m.mood as mood, m.health as health;",request.response());
				} else if (request.params().contains("class")){
					neo.send("START n=node(*),m=node(*) MATCH n<--m WHERE has(m.type) "
						+ "AND has(n.id) AND n.id='" + request.params().get("class") + "' "
						+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
						+ "RETURN m.id as userId,m.ENTPersonNom as firstName, m.ENTPersonPrenom as lastName, "
						+ "m.ENTPersonNomAffichage as displayName, n.id as classId", request.response());
				}
			}
		});
	}
}