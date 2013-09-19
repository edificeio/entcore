package edu.one.core.infra;

import java.io.IOException;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import edu.one.core.infra.request.CookieHelper;
import edu.one.core.infra.security.SecuredAction;

public abstract class Server extends Verticle {

	public Logger log;
	public JsonObject config;
	public RouteMatcher rm;
	public TracerHelper trace;
	private I18n i18n;
	protected Map<String, SecuredAction> securedActions;

	@Override
	public void start() {
		super.start();
		log = container.logger();
		if (config == null) {
			config = container.config();
		}
		rm = new RouteMatcher();
		trace = new TracerHelper(Server.getEventBus(vertx), "log.address", this.getClass().getSimpleName());
		i18n = I18n.getInstance();
		i18n.init(container, vertx);
		CookieHelper.getInstance().init((String) vertx
				.sharedData().getMap("server").get("signKey"), log);

		log.info("Verticle: " + this.getClass().getSimpleName() + " starts on port: " + config.getInteger("port"));

		final String prefix = getPathPrefix(config);
		// Serve public static resource like img, css, js. By convention in /public directory
		// Dummy impl
		rm.getWithRegEx(prefix.replaceAll("\\/", "\\/") + "\\/public\\/.+",
				new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response().sendFile("." + request.path().substring(prefix.length()));
			}
		});

		rm.get(prefix + "/i18n", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				Controller.renderJson(request, i18n.load(request.headers().get("Accept-Language")));
			}
		});

		rm.get(prefix + "/monitoring", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest event) {
				Controller.renderJson(event, new JsonObject().putString("test", "ok"));
			}
		});

		try {
			JsonObject application = new JsonObject()
			.putString("name", config.getString("app-name", this.getClass().getSimpleName()))
			.putString("icon", config.getString("app-icon", ""))
			.putString("address", config.getString("app-address", ""));
			JsonArray actions = StartupUtils.loadSecuredActions();
			securedActions = StartupUtils.securedActionsToMap(actions);
			StartupUtils.sendStartup(application, actions,
					Server.getEventBus(vertx), config.getString("app-registry.address", "wse.app.registry"));
		} catch (IOException e) {
			log.error("Error application not registred.", e);
		}
		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}


	/**
	 * @deprecated Use request.formAttributes() instead
	 * @param request http request
	 * @param handler receive attributes
	 */
	public void bodyToParams(final HttpServerRequest request, final Handler<MultiMap> handler) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				handler.handle(request.formAttributes());
			}
		});
	}

	public static String getPathPrefix(JsonObject config) {
		String path = config.getString("path-prefix");
		if (path == null) {
			String verticle = config.getString("main");
			if (verticle != null && !verticle.trim().isEmpty() && verticle.contains(".")) {
				path = verticle.substring(verticle.lastIndexOf('.') + 1).toLowerCase();
			}
		}
		if ("".equals(path) || "/".equals(path)) {
			return "";
		}
		return "/" + path;
	}

	// TODO serviceloader
	public static EventBus getEventBus(Vertx vertx) {
		//return new EventBusWithLogger(vertx.eventBus());
		return new EventBusWithMongoDBLogger(vertx.eventBus());
	}

}
