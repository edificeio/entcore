/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core;

import edu.one.core.infra.Controller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author rafik
 */
public class History extends Controller {//Verticle implements Handler<Message<String>> {
	private org.vertx.java.core.logging.Logger log;
	private JsonObject config;

	@Override
	public void start() throws Exception {
		super.start();
		
		rm.get("/history/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/history/admin/logs", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				System.out.println("PARAMS : " + request.params().toString());
				String str = "";
				try {
					str = vertx.fileSystem().readFileSync(Paths.get(new File(".").getCanonicalPath() + "/data/dev/"+request.params().get("app")+".trace").toString()).toString();
				} catch (Exception ex) {
					log.info(ex);
				}
				System.out.println("STR = " + str);
				request.response.setChunked(true);
				request.response.write(str);
				request.response.end();
			}
		});
		
	}

}
