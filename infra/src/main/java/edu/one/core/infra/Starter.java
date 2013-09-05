package edu.one.core.infra;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.google.common.io.CharStreams;

public class Starter extends Server {

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
			deployModule(config.getObject("neo4j-persistor"), new Handler<AsyncResult<String>>() {
				@Override
				public void handle(AsyncResult<String> event) {
					if (event.succeeded()) {
						initAutoIndex(config.getObject("neo4j-persistor")
								.getObject("config", new JsonObject()));
						deployModule(config.getObject("app-registry"),
								new Handler<AsyncResult<String>>() {
							@Override
							public void handle(AsyncResult<String> event) {
								if (event.succeeded()) {
									deployModules(config.getArray("external-modules"));
									deployModules(config.getArray("one-modules"));
								}
							}
						});
					}
				}
			});
		} catch (Exception ex) {
			log.error(ex.getMessage());
		}

	}

	private void deployModule(JsonObject module, Handler<AsyncResult<String>> handler) {
		if (module.getString("name") == null) {
			return;
		}
		JsonObject conf = module.getObject("config");
		if (conf == null) {
			try {
				conf = getConfig("../" + module.getString("name") + "/", "mod.json");
			} catch (Exception e) {
				return;
			}
		}
		container.deployModule(module.getString("name"),
				conf, module.getInteger("instances", 1), handler);
	}

	private void deployModules(JsonArray modules) {
		for (Object o : modules) {
			JsonObject module = (JsonObject) o;
			if (module.getString("name") == null) {
				continue;
			}
			JsonObject conf = module.getObject("config");
			if (conf == null) {
				try {
					conf = getConfig("../" + module.getString("name") + "/", "mod.json");
				} catch (Exception e) {
					continue;
				}
			}
			container.deployModule(module.getString("name"),
					conf, module.getInteger("instances", 1));
		}
	}

	private void initAutoIndex(JsonObject config) {
		// FIXME poor hack to create auto index -> replace with equivalent : index --create node_auto_index -t Node
		String query = "CREATE (n {id:'" + UUID.randomUUID().toString() + "'}) DELETE n";
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		Server.getEventBus(vertx).send(config.getString("address", "wse.neo4j.persistor"), jo);
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
