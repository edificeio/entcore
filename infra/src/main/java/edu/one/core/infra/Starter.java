package edu.one.core.infra;

import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.http.Renders;

public class Starter extends Server {

	Neo neo;
	String developerId = "";

	@Override
	public void start() {
		try {
			if (vertx.fileSystem().existsSync("../../developer.id")) {
				developerId = vertx.fileSystem().readFileSync("../../developer.id").toString().trim();
			}

			config = getConfig("", "mod.json");
			super.start();
			vertx.sharedData().getMap("server").put("signKey",
					config.getString("key", "zbxgKWuzfxaYzbXcHnK3WnWK") + Math.random());
			neo = new Neo(vertx.eventBus(),log);
			final String neo4jPersistor = config.getString("neo4j-persistor");
			container.deployModule(neo4jPersistor, getConfig("../" +
					neo4jPersistor + "/", "mod.json"), 1, new Handler<AsyncResult<String>>() {
				@Override
				public void handle(AsyncResult<String> event) {
					if (event.succeeded()) {
						String appRegistryModule = config.getString("app-registry");
						try {
							initAutoIndex(getConfig("../" + neo4jPersistor + "/", "mod.json"));
							container.deployModule(appRegistryModule, getConfig("../" +
									appRegistryModule + "/", "mod.json"), 1, new Handler<AsyncResult<String>>() {
								@Override
								public void handle(AsyncResult<String> event) {
									if (event.succeeded()) {
										try {
											deployExternalModules();
											deployApps();
										} catch (Exception e) {
											log.error(e.getMessage(), e);
										}
									}
								}
							});
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			});
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}

		final Renders render = new Renders(container);

			rm.get("/infra/starter/dev", new Handler<HttpServerRequest> () {
				public void handle(HttpServerRequest request) {
					render.renderView(request);
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

	private void deployExternalModules() {
		for (Object o : config.getArray("external-modules")){
			JsonObject module = (JsonObject)o ;
			if (module.getString("name") != null && module.getObject("config") != null) {
				container.deployModule(module.getString("name"),
						module.getObject("config"), module.getInteger("instances", 1));
			}
		}
	}


	private void initAutoIndex(JsonObject config) {
		// FIXME poor hack to create auto index -> replace with equivalent : index --create node_auto_index -t Node
		String query = "CREATE (n {id:'" + UUID.randomUUID().toString() + "'}) DELETE n";
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		vertx.eventBus().send(config.getString("address"), jo);
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
