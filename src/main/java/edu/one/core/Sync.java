package edu.one.core;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class Sync extends Verticle {
	private Logger log;
	private JsonObject config;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info(container.getConfig().getString("test"));
		config = container.getConfig();

				vertx.eventBus().send("wse.neo4j.persistor"
					, new JsonObject().putString("action", "query").putString("query", "START n=node(1) RETURN n"));

		RouteMatcher rm = new RouteMatcher();
		rm.get("sync/data/dev", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {

				Buffer setupData = null;
				try {
					setupData = vertx.fileSystem().readFileSync(config.getString("dev-data-file"));
				} catch (Exception ex) {
					log.error("dev-data-file not loaded");
					req.response.statusCode = 500;
					req.response.end(new JsonObject().putString("error", "dev-data-file not loaded").encode());
				}
				req.response.end();
			}
		});

		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}
}