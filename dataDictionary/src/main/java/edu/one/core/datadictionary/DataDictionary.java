package edu.one.core.datadictionary;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class DataDictionary extends Controller {

	@Override
	public void start()  {
		super.start();

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});

		rm.get("/dictionary/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				request.response().putHeader("content-type", "text/json");
				request.response().sendFile(request.params().get("id") + "-dictionary.json");
			}
		});
	}

}