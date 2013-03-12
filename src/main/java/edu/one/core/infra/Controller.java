/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.infra;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import edu.one.core.infra.mustache.I18nTemplateFunction;
import edu.one.core.infra.mustache.StaticResourceTemplateFunction;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public abstract class Controller extends Verticle {

	public Logger log;
	public JsonObject config;
	public RouteMatcher rm;
	private MustacheFactory mf;
	private I18n i18n;

	@Override
	public void start() throws Exception {
		super.start();
		log = container.getLogger();
		config = container.getConfig();
		rm = new RouteMatcher();
		mf = new DefaultMustacheFactory("./view");
		i18n = new I18n(container, vertx);

		log.info("Verticle:" + this.getClass().getSimpleName() + " starts on port:" + config.getInteger("port"));

		// Serve public static resource like img, css, js. By convention in /public directory
		// Dummy impl
		rm.getWithRegEx("\\/public\\/.+", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response.sendFile("." + request.path);
			}
		});
		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}

	private Map<String,Object> functionsScope(HttpServerRequest request) {
		Map<String,Object> scope = new HashMap<>();
		scope.put("static", new StaticResourceTemplateFunction(request));
		scope.put("i18n", new I18nTemplateFunction(i18n, request));
		return scope;
	}
	/*
	 * Render a Mustache template : see http://mustache.github.com/mustache.5.html
	 * TODO : modularize
	 * TODO : isolate sscope management 
	 */
	public void renderView(HttpServerRequest request, JsonObject params) {
		try {
			Mustache mustache = mf.compile(request.path + ".html");
			Writer writer = new StringWriter();
			Object[] scopes = { params.toMap(), functionsScope(request)};
			mustache.execute(writer, scopes).flush();
			request.response.end(writer.toString());
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public void renderJson(HttpServerResponse response, JsonObject jo) {
		response.putHeader("content-type", "text/json");
		response.end(jo.encode());
	}

	public void redirect(HttpServerRequest request, String location) {
		redirect(request, request.headers().get("Host"), location);
	}

	public void redirect(HttpServerRequest request, String host, String location) {
		request.response.statusCode = 301;
		request.response.putHeader("Location", host + location);
		request.response.end();
	}

	public void bodyToParams(final HttpServerRequest request, final Handler<Map<String,String>> handler) {
		if (Arrays.asList("POST","PUT").contains(request.method)) {
			request.bodyHandler(new Handler<Buffer>() {
				public void handle(Buffer b) {
					Map<String, String> postParams = new HashMap<>();
					try {
						for(String keyVal : b.toString().split("&")) {
							String[] entry = keyVal.split("=");
							postParams.put(entry[0],URLDecoder.decode(entry[1], "UTF-8"));
						}
					} catch (UnsupportedEncodingException ex) {
						log.error(ex.getMessage());
					}
					handler.handle(postParams);
				}
			});
		}
	}
}
