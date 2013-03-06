/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
>>>>>>> 7285ff9... [History] basic log display

/**
 *
 * @author rafik
 */
public class History extends Verticle implements Handler<Message<String>> {
	private Logger log;

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info(container.getConfig().getString("test"));
		
		RouteMatcher rm = new RouteMatcher();
		rm.get("/", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {
				try {
					req.response.sendFile(new File(".").getCanonicalPath() + "/data/dev/" + req.params().get("app").toLowerCase() + ".trace");
				} catch (IOException ex) {
					java.util.logging.Logger.getLogger(History.class.getName()).log(Level.SEVERE, null, ex);
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
