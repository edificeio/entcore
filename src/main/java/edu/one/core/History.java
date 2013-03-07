/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import java.io.File;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
>>>>>>> 7285ff9... [History] basic log display

/**
 *
 * @author rafik
 */
public class History extends Verticle implements Handler<Message<String>> {
	private org.vertx.java.core.logging.Logger log;
	private JsonObject config;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		config = container.getConfig();
		log.info(config.getString("test"));
		
		RouteMatcher rm = new RouteMatcher();
		rm.get("/", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {
				try {
					if (req.params().isEmpty()){
						req.response.sendFile("view/logs/logs.html");
					} else {
						req.response.sendFile(new File(".").getCanonicalPath() + config.getString("log-path") + req.params().get("app").toLowerCase() + ".trace");
					}
				} catch (Exception ex) {
					log.info(ex);
				}
			}
		});
		vertx.createHttpServer().requestHandler(rm).listen(container.getConfig().getInteger("port"));

	}

	@Override
	public void handle(Message<String> event) {
		log.info(event.body);
	}

}
