/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.infra;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import edu.one.core.infra.mustache.DevMustacheFactory;
import edu.one.core.infra.mustache.I18nTemplateFunction;
import edu.one.core.infra.mustache.StaticResourceTemplateFunction;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public abstract class Controller extends Verticle {

	public Logger log;
	public JsonObject config;
	public RouteMatcher rm;
	public TracerHelper trace;
	private MustacheFactory mf;
	private I18n i18n;

	@Override
	public void start() {
		super.start();
		log = container.logger();
		if (config == null) {
			config = container.config();
		}
		rm = new RouteMatcher();
		trace = new TracerHelper(vertx.eventBus(), "log.address", this.getClass().getSimpleName());
		mf = "dev".equals(config.getString("mode")) ? new DevMustacheFactory("./view") : new DefaultMustacheFactory("./view");
		i18n = new I18n(container, vertx);

		log.info("Verticle: " + this.getClass().getSimpleName() + " starts on port: " + config.getInteger("port"));

		// Serve public static resource like img, css, js. By convention in /public directory
		// Dummy impl
		rm.getWithRegEx("\\/public\\/.+", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response().sendFile("." + request.path());
			}
		});

		rm.get("/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, i18n.load(request.headers().get("Accept-Language")));
			}
		});

		rm.get("/monitoring", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest event) {
				renderJson(event, new JsonObject().putString("test", "ok"));
			}
		});

		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}

	private Map<String,Object> functionsScope(HttpServerRequest request) {
		Map<String,Object> scope = new HashMap<>();
		scope.put("infra", new StaticResourceTemplateFunction(request, "8001")); // FIXME get port from infra module
		scope.put("static", new StaticResourceTemplateFunction(request));
		scope.put("i18n", new I18nTemplateFunction(i18n, request));
		return scope;
	}

	public void renderView(HttpServerRequest request) {
		renderView(request, new JsonObject());
	}

	/*
	 * Render a Mustache template : see http://mustache.github.com/mustache.5.html
	 * TODO : modularize
	 * TODO : isolate sscope management 
	 */
	public void renderView(HttpServerRequest request, JsonObject params) {
		try {
			if (params == null) { params = new JsonObject(); }
			Mustache mustache = mf.compile(request.path() + ".html");
			Writer writer = new StringWriter();
			Object[] scopes = { params.toMap(), functionsScope(request)};
			mustache.execute(writer, scopes).flush();
			request.response().end(writer.toString());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			renderError(request);
		}
	}

	public void renderError(HttpServerRequest request) {
		request.response().setStatusCode(500);
		request.response().end();
	}

	public void renderJson(HttpServerRequest request, JsonObject jo) {
		request.response().putHeader("content-type", "text/json");
		request.response().end(jo.encode());
	}

	public void redirect(HttpServerRequest request, String location) {
		redirect(request, request.headers().get("Host"), location);
	}

	public void redirect(HttpServerRequest request, String host, String location) {
		request.response().setStatusCode(301);
		request.response().putHeader("Location", host + location);
		request.response().end();
	}

	/**
	 * @deprecated Use request.formAttributes() instead
	 * @param request http request
	 * @param handler receive attributes
	 */
	public void bodyToParams(final HttpServerRequest request, final Handler<MultiMap> handler) {
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				handler.handle(request.formAttributes());
			}
		});
	}

}
