package edu.one.core;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Directory extends Controller{

	@Override
	public void start() throws Exception {
		super.start();
		final JsonObject dataMock = new JsonObject(vertx.fileSystem().readFileSync("directory-data-mock.json").toString());
		
		rm.get("/directory/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/directory/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request.response, dataMock.getObject("ecole"));

			}
		});
	}

}