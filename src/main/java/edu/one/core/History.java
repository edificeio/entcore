/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
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
				String html = "<!DOCTYPE html><html><head><title>One : Logs</title></head>"
						+ "<body><form method=\"GET\" action=\"http://localhost:8006/\">"
						+ "<select name=\"app\"><option>all</option><option>sync</option>"
						+ "<option>history</option><option>directory</option></select>"
						+ "<input type=\"submit\" value=\"see logs\"/></form>";
				req.response.setChunked(true);
				if (req.params().isEmpty()){
					req.response.write(html);
					req.response.end("</body></html>");
				} else {
					req.response.write(html.replace("<option>"+req.params().get("app").toLowerCase()
							, "<option selected=\"selected\">"+req.params().get("app").toLowerCase()));
					req.response.write("<p>");
					try {
						Path readFile = Paths.get(new File(".").getCanonicalPath() + config.getString("log-path") 
								+ req.params().get("app").toLowerCase() + ".trace");
						BufferedReader br = Files.newBufferedReader(readFile, Charset.forName("UTF-8"));
						String thisLine;
						while ((thisLine = br.readLine()) != null) {
							req.response.write(thisLine + "\n");
						}
						br.close();
					} catch (IOException ex) {
						log.info(ex);
					}
					req.response.write("</p>");
					req.response.end("</body></html>");
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
