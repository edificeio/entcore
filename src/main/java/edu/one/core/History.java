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
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author rafik
 */
public class History extends Controller {

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
				String str = "";
				try {
					str = vertx.fileSystem().readFileSync(Paths.get(new File(".").getCanonicalPath() + "/data/dev/"+request.params().get("app")+".trace").toString()).toString();
				} catch (Exception ex) {
					log.info(ex);
				}
				JsonArray arr = new JsonArray();
				for (String string : str.split("}")) {
					arr.add(new JsonObject(string + "}"));
				}
				request.response.putHeader("content-type", "text/json");
				request.response.end(arr.encode());
			}
		});
		
	}

}
