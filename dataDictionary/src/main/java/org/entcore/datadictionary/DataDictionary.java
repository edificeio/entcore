package org.entcore.datadictionary;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class DataDictionary extends Server {

	@Override
	public void start()  {
		super.start();
		final Renders render = new Renders(vertx, container);

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request, new JsonObject());
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