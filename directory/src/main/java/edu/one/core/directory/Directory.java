package edu.one.core.directory;

import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Directory extends Controller {
	
	JsonObject dataMock;
	Neo neo;

	@Override
	public void start() throws Exception {
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		dataMock = new JsonObject(vertx.fileSystem().readFileSync("directory-data-mock.json").toString());
		container.deployModule("edu.one~wordpress~1.0.0-SNAPSHOT");
		
		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*) WHERE has(n.type) "
						+ "AND n.type='ETABEDUCNAT' "
						+ "RETURN distinct n.ENTStructureNomCourant, n.id", request.response);
			}
		});
		
		rm.get("/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String schoolId = request.params().get("id");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + schoolId + "' "
						+ "AND has(m.type) AND m.type='CLASSE' "
						+ "RETURN distinct m.ENTGroupeNom, m.id, n.id", request.response);
			}
		});
		
		rm.get("/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("id").replaceAll("-", " ").replaceAll("_", "\\$");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + classId + "' "
						+ "AND has(m.type) and m.type='ELEVE' "
						+ "RETURN distinct m.id,m.ENTPersonNom,m.ENTPersonPrenom, n.id", request.response);
			}
		});

		rm.get("/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String personId = request.params().get("id");
				neo.send("START n=node(*) WHERE has(n.id) "
						+ "AND n.id='" + personId + "' "
						+ "RETURN distinct n.ENTPersonNom, n.ENTPersonPrenom, n.ENTPersonAdresse", request.response);
			}
		});

		rm.get("/api/load/schools", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				final JsonArray ecoles = dataMock.getArray("ecoles");
				for (Object object : ecoles) {
					final JsonObject ecole = ((JsonObject)object);
					log.info("Creating a school");
					vertx.eventBus().send(config.getString("bus-address"), ecole, new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> message) {
							if (message.body.getString("status").equals("ok")){
								log.info(message.body);
							}
						}
					});
				}
				request.response.end("Écoles chargées");
			}
		});

		rm.get("/api/load/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				final JsonArray classes = dataMock.getArray("classes");
				for (Object object : classes) {
					final JsonObject classe = ((JsonObject)object);
					log.info("Creating a class");
					vertx.eventBus().send(config.getString("bus-address"), classe, new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> message) {
							if (message.body.getString("status").equals("ok")){
								log.info(message.body);
							}
						}
					});
				}
				request.response.end("Classes chargées");
			}
		});

		rm.get("/api/load/users", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonArray personnes = dataMock.getArray("personnes");
				for (Object object : personnes) {
					final JsonObject personne = ((JsonObject)object);
					log.info("Creating user");
					vertx.eventBus().send(config.getString("bus-address"), personne, new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> message) {
							if (message.body.getString("status").equals("ok")){
								log.info(message.body);
							}
						}
					});
				}
				request.response.end("Utilisateurs chargés");
			}
		});

		rm.get("/api/load/pages", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonArray pages = dataMock.getArray("pages");
				for (Object object : pages) {
					final JsonObject page = ((JsonObject)object);
					log.info("Creating page");
					vertx.eventBus().send(config.getString("bus-address"), page, new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> message) {
							log.info(message.body);
						}
					});
				}
				request.response.end("Utilisateurs chargés");
			}
		});
	}

}
