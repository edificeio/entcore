package edu.one.core.infra;


import edu.one.core.Admin;
import edu.one.core.AppRegistry;
import edu.one.core.Directory;
import edu.one.core.History;
import edu.one.core.module.Neo4jPersistor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class Starter extends Verticle{

	private Logger log;
	JsonObject config;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info("Applications bootstrap");
		config = getConfig();
		deployApps();

		RouteMatcher rm = new RouteMatcher();

		rm.get("/start/dev", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {
				req.response.sendFile("./resources/view/dev.html");
			}
		});

		// TODO : choice to wrrap redirect in function Or to keep HTTP protocol detail visible
		rm.getWithRegEx(".*", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {
				req.response.statusCode = 301;
				req.response.headers().put("Location", req.headers().get("Host") + "start/dev");
				req.response.end();
			}
		});

		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}

	private void deployApps() throws Exception {
		// TODO : extract Neo4jPersistor in a standalone module
		Buffer  b = vertx.fileSystem().readFileSync("mod-neo4j-persistor.json");
		if (b == null) { 
			log.error("Configuration file mod-neo4j-persistor.json not found");
			return;
		}
		container.deployVerticle(Neo4jPersistor.class.getName(), new JsonObject(b.toString()));

		container.deployVerticle(Admin.class.getName(), config.getObject("Admin.conf"));
		container.deployVerticle(Directory.class.getName(), config.getObject("Directory.conf"));
		container.deployVerticle(AppRegistry.class.getName(), config.getObject("Application.conf"));
		container.deployVerticle(History.class.getName(), config.getObject("History.conf"));
	}

	private JsonObject getConfig() throws Exception {
		Buffer b = vertx.fileSystem().readFileSync("mod.json");
		if (b == null) { 
			log.error("Configuration file mod.json not found");
			throw new Exception("Configuration file mod.json not found");
		}
		else {
			return new JsonObject(b.toString());
		}
	}
}