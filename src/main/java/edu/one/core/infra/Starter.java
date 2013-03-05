package edu.one.core.infra;
import edu.one.core.Admin;
import edu.one.core.AppRegistry;
import edu.one.core.Directory;
import edu.one.core.History;
import edu.one.core.Sync;
import edu.one.core.module.Neo4jPersistor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Starter extends Controller {

	Neo neo;

	@Override
	public void start() throws Exception {
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
		container.deployVerticle(Neo4jPersistor.class.getName(), getConfig("mod-neo4j-persistor.json"));
		container.deployVerticle(Admin.class.getName(), config.getObject("Admin.conf"));
		container.deployVerticle(Directory.class.getName(), config.getObject("Directory.conf"));
		container.deployVerticle(AppRegistry.class.getName(), config.getObject("Application.conf"));
		container.deployVerticle(History.class.getName(), config.getObject("History.conf"));
		container.deployVerticle(edu.one.core.module.Tracer.class.getName(), config.getObject("Tracer.conf"));
		container.deployVerticle(Sync.class.getName(), config.getObject("Sync.conf"));
	}

	private JsonObject getConfig(String fileName) throws Exception {
		Buffer b = vertx.fileSystem().readFileSync(fileName);
		if (b == null) { 
			log.error("Configuration file "+ fileName +"not found");
			throw new Exception("Configuration file "+ fileName +" not found");
		}
		else {
			return new JsonObject(b.toString());
		}
	}
}
