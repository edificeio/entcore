package edu.one.core.userBook;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
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

		rm.get("/load", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, userBookData);
			}
		});

		rm.get("/classe", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				getDirectoryData("/api/personnes?id=4400000002$ORDINAIRE$CM2%20de%20Mme%20Rousseau");
				renderView(request, users);
			}
		});
		
		rm.get("/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				HttpClient client = vertx.createHttpClient().setPort(8003);
				HttpClientRequest req = client.get("/api/details?id=" + request.params().get("id"), new Handler<HttpClientResponse>() {
					public void handle(HttpClientResponse resp) {
						resp.bodyHandler(new Handler<Buffer>() {
							public void handle(Buffer data) {
								renderJson(request, new JsonObject(data.toString()));
							}
						});
					}
				});
				req.end();
			}
		});

		rm.get("/load-class", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, users);
			}
		});

		rm.get("/api/userbook", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*),m=node(*) MATCH n-[:USERBOOK]->m "
						+ "WHERE has(n.ENTPersonIdentifiant) AND n.ENTPersonIdentifiant='"
						+ request.params().get("id") +"' RETURN m.health as health,"
						+ " m.mood as mood, m.motto as motto", request.response());
			}
		});
	}

	private void getDirectoryData(String apiUrl){
		HttpClient client = vertx.createHttpClient().setPort(8003);
		HttpClientRequest req = client.get(apiUrl, new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse resp) {
				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer data) {
						users = new JsonObject(data.toString());
					}
				});
			}
		});
		req.end();
	}
}