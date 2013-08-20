package edu.one.core.portal;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class Portal extends Server {

	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(container);

		rm.get("/portal", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request);
			}
		});

	}

}
