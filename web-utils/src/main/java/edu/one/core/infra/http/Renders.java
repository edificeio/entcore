package edu.one.core.infra.http;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import edu.one.core.infra.I18n;
import edu.one.core.infra.mustache.DevMustacheFactory;
import edu.one.core.infra.mustache.I18nTemplateFunction;
import edu.one.core.infra.mustache.StaticResourceTemplateFunction;

public class Renders {

	private final MustacheFactory mf;
	protected final Logger log;

	public Renders(Container container) {
		this.log = container.logger();
		this.mf = "dev".equals(container.config().getString("mode"))
				? new DevMustacheFactory("./view") : new DefaultMustacheFactory("./view");
	}

	private Map<String,Object> functionsScope(HttpServerRequest request) {
		Map<String,Object> scope = new HashMap<>();
		scope.put("infra", new StaticResourceTemplateFunction(request, "8001")); // FIXME get port from infra module
		scope.put("static", new StaticResourceTemplateFunction(request));
		scope.put("i18n", new I18nTemplateFunction(I18n.getInstance(), request));
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
		renderView(request, params, null, null);
	}

	public void renderView(HttpServerRequest request, JsonObject params, String resourceName, Reader r) {
		try {
			if (params == null) { params = new JsonObject(); }
			Mustache mustache;
			if (resourceName != null && r != null && !resourceName.trim().isEmpty()) {
				mustache = mf.compile(r, resourceName);
			} else if (resourceName != null && !resourceName.trim().isEmpty()) {
				mustache = mf.compile(resourceName);
			} else {
				mustache = mf.compile(request.path() + ".html");
			}
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

	public static void badRequest(HttpServerRequest request) {
		request.response().setStatusCode(400).end();
	}

	public static void unauthorized(HttpServerRequest request) {
		request.response().setStatusCode(401).end();
	}

	public static void renderError(HttpServerRequest request, JsonObject error) {
		request.response().setStatusCode(500);
		if (error != null) {
			request.response().end(error.encode());
		} else {
			request.response().end();
		}
	}

	public static void renderError(HttpServerRequest request) {
		renderError(request, null);
	}

	public static void renderJson(HttpServerRequest request, JsonObject jo, int status) {
		request.response().putHeader("content-type", "text/json");
		request.response().setStatusCode(status);
		request.response().end(jo.encode());
	}

	public static void renderJson(HttpServerRequest request, JsonObject jo) {
		renderJson(request, jo, 200);
	}

	public static void renderJson(HttpServerRequest request, JsonArray jo) {
		request.response().putHeader("content-type", "text/json");
		request.response().end(jo.encode());
	}

	public static void redirect(HttpServerRequest request, String location) {
		redirect(request, "http://" + request.headers().get("Host"), location);
	}

	public static void redirect(HttpServerRequest request, String host, String location) {
		request.response().setStatusCode(302);
		request.response().putHeader("Location", host + location);
		request.response().end();
	}

	public static void redirectPermanent(HttpServerRequest request, String location) {
		redirectPermanent(request, "http://" + request.headers().get("Host"), location);
	}

	public static void redirectPermanent(HttpServerRequest request, String host, String location) {
		request.response().setStatusCode(301);
		request.response().putHeader("Location", host + location);
		request.response().end();
	}

}
