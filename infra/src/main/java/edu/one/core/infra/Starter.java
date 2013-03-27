package edu.one.core.infra;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Starter extends Controller {

	Neo neo;
	String developerId = "";

	@Override
	public void start() throws Exception {
		developerId = vertx.fileSystem().readFileSync("../../developer.id").toString().trim();
		config = getConfig("mod.json");
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		deployApps();

		rm.get("/starter/dev", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

		rm.get("/starter/test", new Handler<HttpServerRequest> () {
			public void handle(final HttpServerRequest request) {
				neo.send(request.params().get("query"), request.response);
			}
		});

	}

	private void deployApps() throws Exception {
		for (String module : config.getString("one-modules").split(",")){
			module = module.trim();
			container.deployModule(module, getConfig("../"+ module +"/mod.json"));
		}
	}

	protected JsonObject getConfig(String fileName) throws Exception {
		Buffer b;
		if (! developerId.isEmpty() && vertx.fileSystem().existsSync(fileName + "." + developerId)) {
			b = vertx.fileSystem().readFileSync(fileName + "." + developerId);
		} else {
			b = vertx.fileSystem().readFileSync(fileName);
		}

		if (b == null) {
			log.error("Configuration file "+ fileName +"not found");
			throw new Exception("Configuration file "+ fileName +" not found");
		}
		else {
			return new JsonObject(b.toString());
		}
	}

}
