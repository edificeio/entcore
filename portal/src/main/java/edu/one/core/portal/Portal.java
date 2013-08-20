package edu.one.core.portal;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;
import edu.one.core.portal.mustache.AssetResourceTemplateFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class Portal extends Server {

	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(container);
		try {
			render.putTemplateFunction("asset", new AssetResourceTemplateFunction(config.getString("skin")));
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}

		rm.get("/portal", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request);
			}
		});

		rm.getWithRegEx("\\/assets\\/.+", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response().sendFile("." + request.path());
			}
		});

	}

}
