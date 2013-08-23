package edu.one.core.admin;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;
import edu.one.core.infra.request.CookieHelper;
import edu.one.core.infra.request.filter.SecurityHandler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class Admin extends Server {

	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(container);

		rm.get("/admin", new SecurityHandler(){
			@Override
			public void filter(HttpServerRequest request) {
				render.renderView(request, config);
			}
		});

		rm.get("/logout", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				CookieHelper.set("oneSessionId", "", request.response());
				Renders.redirect(request, "localhost:8009", "/login");
			}
		});

	}

}
