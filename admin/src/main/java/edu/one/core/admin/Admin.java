package edu.one.core.admin;

import edu.one.core.infra.Controller;
import edu.one.core.infra.request.CookieUtils;
import edu.one.core.infra.request.filter.SecurityHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class Admin extends Controller {

	@Override
	public void start() {
		super.start();

		rm.get("/admin", new SecurityHandler(){
			@Override
			public void filter(HttpServerRequest request) {
				renderView(request, config);
			}
		});

		rm.get("/logout", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				CookieUtils.set("oneSessionId", "", request.response());
				redirect(request, "localhost:8009", "/login");
			}
		});

	}

}
