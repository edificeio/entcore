/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.infra;

import fr.wseduc.webutils.Server;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

public class Starter extends Server {

	String developerId = "";

	@Override
	public void start() {
		try {
			if (vertx.fileSystem().existsSync("../../developer.id")) {
				developerId = vertx.fileSystem().readFileSync("../../developer.id").toString().trim();
			}
			if (container.config() == null || container.config().size() == 0) {
				config = getConfig("", "mod.json");
			}
			super.start();
			vertx.sharedData().getMap("server").put("signKey",
					config.getString("key", "zbxgKWuzfxaYzbXcHnK3WnWK") + Math.random());
			deployModule(config.getObject("neo4j-persistor"), false, new Handler<AsyncResult<String>>() {
				@Override
				public void handle(AsyncResult<String> event) {
					if (event.succeeded()) {
						loadCypherScript(); // only in dev mode with embedded neo4j
						deployModule(config.getObject("app-registry"), false,
								new Handler<AsyncResult<String>>() {
							@Override
							public void handle(AsyncResult<String> event) {
								if (event.succeeded()) {
									deployModules(config.getArray("external-modules", new JsonArray()), false);
									deployModules(config.getArray("one-modules", new JsonArray()), true);
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

	private void deployModule(JsonObject module, boolean internal, Handler<AsyncResult<String>> handler) {
		if (module.getString("name") == null) {
			return;
		}
		JsonObject conf = new JsonObject();
		if (internal) {
			try {
				conf = getConfig("../" + module.getString("name") + "/", "mod.json");
			} catch (Exception e) {
				log.error("Invalid configuration for module " + module.getString("name"), e);
				return;
			}
		}
		conf = conf.mergeIn(module.getObject("config", new JsonObject()));
		container.deployModule(module.getString("name"),
				conf, module.getInteger("instances", 1), handler);
	}

	private void deployModules(JsonArray modules, boolean internal) {
		for (Object o : modules) {
			JsonObject module = (JsonObject) o;
			if (module.getString("name") == null) {
				continue;
			}
			JsonObject conf = new JsonObject();
			if (internal) {
				try {
					conf = getConfig("../" + module.getString("name") + "/", "mod.json");
				} catch (Exception e) {
					log.error("Invalid configuration for module " + module.getString("name"), e);
					continue;
				}
			}
			conf = conf.mergeIn(module.getObject("config", new JsonObject()));
			container.deployModule(module.getString("name"),
					conf, module.getInteger("instances", 1));
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

	private void loadCypherScript() {
		if ("dev".equals(config.getString("mode"))) {
			String neo4jServerUri = config.getObject("neo4j-persistor", new JsonObject())
					.getObject("config", new JsonObject()).getString("server-uri");
			final String scriptsFolder = config.getString("scripts-folder");
			if ((neo4jServerUri == null || neo4jServerUri.isEmpty()) && scriptsFolder != null) {
				execute("MATCH (n:System) WHERE n.name = 'neo4j' RETURN n.scripts as scripts", null,
						new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							JsonArray res = event.body().getArray("result");
							if (log.isDebugEnabled() && res != null) {
								log.debug(res.encode());
							}
							Handler<JsonArray> handler;
							JsonArray executedScripts;
							if (res != null && res.size() == 0) {
								executedScripts = new JsonArray();
								handler = new Handler<JsonArray>() {
									@Override
									public void handle(JsonArray scripts) {
										execute("CREATE (n:System {scripts : {scripts}, name : 'neo4j'})",
												new JsonObject().putArray("scripts", scripts), null);
									}
								};
								executeCypherScript(scriptsFolder, executedScripts, handler);
							} else if (res != null) {
								executedScripts = ((JsonObject) res.get(0)).getArray("scripts", new JsonArray());
								handler = new Handler<JsonArray>() {
									@Override
									public void handle(JsonArray scripts) {
										execute("MATCH (n:System) WHERE n.name = 'neo4j' SET n.scripts = {scripts}}",
												new JsonObject().putArray("scripts", scripts), null);
									}
								};
								executeCypherScript(scriptsFolder, executedScripts, handler);
							}
						}
					}
				});
			}
		}
	}

	private void executeCypherScript(String folder, final JsonArray executedScripts,
			final Handler<JsonArray> handler) {
		if (folder == null || folder.isEmpty()) return;
		vertx.fileSystem().readDir(folder, ".*?cypher$", new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(AsyncResult<String[]> ar) {
				if (ar.succeeded()) {
					final AtomicInteger count = new AtomicInteger(ar.result().length);
					for (String path: ar.result()) {
						if (executedScripts.contains(path)) continue;
						executedScripts.add(path);
						vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {
							@Override
							public void handle(AsyncResult<Buffer> ar) {
								if (ar.succeeded()) {
									String queries = ar.result().toString("UTF-8")
											.replaceAll("\n", "")
											.replaceAll("begin transaction", "")
											.replaceAll("commit", "");
									for (String query : queries.split(";")) {
										execute(query, null, null);
									}
								}
								if (count.decrementAndGet() == 0) {
									handler.handle(executedScripts);
								}
							}
						});
					}
				} else {
					log.error(ar.cause());
				}
			}
		});
	}

	private void execute(String query, JsonObject params, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		if (params != null) {
			jo.putObject("params", params);
		}
		vertx.eventBus().send("wse.neo4j.persistor", jo, handler);
	}

}
