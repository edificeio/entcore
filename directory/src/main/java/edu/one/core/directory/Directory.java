package edu.one.core.directory;

import edu.one.core.infra.Controller;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Directory extends Controller {
	
	JsonObject dataMock;

	@Override
	public void start() throws Exception {
		super.start();
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
				request.response.putHeader("content-type", "text/json");
				request.response.end(dataMock.getArray("ecoles").encode());
				//renderJson(request, dataMock.getObject("ecole"));
				
			}
		});
		
		rm.get("/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				request.response.putHeader("content-type", "text/json");
				request.response.end(dataMock.getArray("classes").encode());
				//renderJson(request.response, dataMock.getObject("classes"));
				
			}
		});
		
		rm.get("/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				request.response.putHeader("content-type", "text/json");
				request.response.end(dataMock.getArray("personnes").encode());
				//renderJson(request.response, dataMock.getObject("personnes"));
				
			}
		});
		
		rm.get("/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonArray people = dataMock.getArray("personnes");
				for (Object object : people) {
					JsonObject jo = (JsonObject)object;
					if (jo.getInteger("id").equals(new Integer(request.params().get("id")))){
						renderJson(request, jo);
					}
				}
			}
		});
		
		rm.post("/api/edit", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				bodyToParams(request, new Handler<Map<String, String>>() {
					@Override
					public void handle(Map<String, String> params) {
						JsonObject jo = new JsonObject().putString("message", "success");
						renderJson(request, jo);
					}
				});
				
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
