package edu.one.core.infra;
/*
	import edu.one.core.Admin;
	import edu.one.core.AppRegistry;
	import edu.one.core.Directory;
	import edu.one.core.History;
	import edu.one.core.Sync;
import edu.one.core.module.Neo4jPersistor;
*/
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Starter extends Controller {

	Neo neo;

	@Override
	public void start() throws Exception {
//		config = getConfig("mod.json");
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
		container.deployModule("com.wse.neo4j~neo4jPersistor~0.1.0-SNAPSHOT", getConfig("../com.wse.neo4j~neo4jPersistor~0.1.0-SNAPSHOT/mod.json"));
		container.deployModule("edu.one.core~tracer~0.1.0-SNAPSHOT", getConfig("../edu.one.core~tracer~0.1.0-SNAPSHOT/mod.json"));
		container.deployModule("edu.one.core~sync~0.1.0-SNAPSHOT");
		container.deployModule("edu.one.core~directory~0.1.0-SNAPSHOT");
		container.deployModule("edu.one.core~history~0.1.0-SNAPSHOT");
	}

}
