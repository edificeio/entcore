package edu.one.core.admin;

import edu.one.core.infra.Controller;
import edu.one.core.infra.request.filter.FilterChainHandler;
import org.vertx.java.core.http.HttpServerRequest;

public class Admin extends Controller {

	@Override
	public void start() throws Exception {
		super.start();

		rm.get("/admin", new FilterChainHandler() {
			@Override
			public void filterAndHandle(HttpServerRequest request) {
				renderView(request, config);
			}
		});

		rm.get("/logout", new FilterChainHandler() {
			@Override
			public void filterAndHandle(HttpServerRequest request) {
				request.response.putHeader("Set-Cookie", "oneID=");
				redirect(request, "localhost:8009", "/login");
			}
		});

	}

}
