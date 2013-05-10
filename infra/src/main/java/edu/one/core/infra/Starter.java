package edu.one.core.infra;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Starter extends Controller {

	Neo neo;
	String developerId = "";

	@Override
	public void start() {
		try {
			developerId = vertx.fileSystem().readFileSync("../../developer.id").toString().trim();
			config = getConfig("", "mod.json");
			super.start();
			neo = new Neo(vertx.eventBus(),log);
			deployApps();
		} catch (Exception ex) {
			log.equals(ex.getMessage());
		}
			rm.get("/starter/dev", new Handler<HttpServerRequest> () {
				public void handle(HttpServerRequest request) {
					renderView(request);
				}
			});

			rm.get("/starter/test", new Handler<HttpServerRequest> () {
				public void handle(final HttpServerRequest request) {
					neo.send(request);
				}
			});

	}

	private void deployApps() throws Exception {
		for (Object o : config.getArray("one-modules")){
			String module = ((String)o).trim();
			if (vertx.fileSystem().existsSync("../" + module)) {
				container.deployModule(module, getConfig("../"+ module + "/", "mod.json"));
			}
		}
	}

	protected JsonObject getConfig(String path, String fileName) throws Exception {
		Buffer b;
		if (! developerId.isEmpty() && vertx.fileSystem().existsSync(path + developerId + "." + fileName)) {
			b = vertx.fileSystem().readFileSync(path + developerId + "." + fileName);
		} else {
			b = vertx.fileSystem().readFileSync(path + fileName);
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
