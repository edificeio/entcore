package edu.one.core.myAccount;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class MyAccount extends Controller {
	
	@Override
	public void start() {
		super.start();
		final JsonObject dataMock = 
				new JsonObject(vertx.fileSystem().readFileSync("myAccount-data-mock.json").toString());

		rm.get("/index", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, dataMock);
			}
		});

		rm.get("/load", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, dataMock);
			}
		});
	}
}
