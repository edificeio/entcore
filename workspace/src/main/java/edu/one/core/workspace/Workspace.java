package edu.one.core.workspace;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Controller;
import edu.one.core.infra.MongoDb;

public class Workspace extends Controller {

	@Override
	public void start() {
		super.start();

		// Mongodb config
		JsonObject mongodbConf = config.getObject("mongodb-config");
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-CR1", mongodbConf);
		final MongoDb mongo = new MongoDb(vertx.eventBus(), mongodbConf.getString("address"));

		rm.get("/workspace/test/:content", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject document = new JsonObject();
				document.putString("content", request.params().get("content"));
				mongo.save("test", document);
				request.response().end();
			}
		});
	}

}
