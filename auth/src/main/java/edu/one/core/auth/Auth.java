package edu.one.core.auth;

import edu.one.core.infra.Controller;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Auth extends Controller {

	@Override
	public void start() throws Exception {
		super.start();

		rm.get("/login", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				if ("admin".equals(request.params().get("email"))
					&& "admin".equals(request.params().get("password"))) {
					redirect(request, "http://localhost:8008", "/admin?oneID=1234");
				} else if (request.params().get("email") != null){
					renderView(request, new JsonObject().putString("error", "true"));
				} else
					renderView(request, config);
			}
		});

		rm.post("/login", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {

			}
		});

		rm.get("/logout", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, config);
			}
		});

	}

}
