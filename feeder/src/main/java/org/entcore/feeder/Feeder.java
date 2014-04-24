package org.entcore.feeder;

import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.be1d.Be1dFeeder;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Feeder extends BusModBase implements Handler<Message<JsonObject>> {

	private Feed feed;
	private ManualFeeder manual;
	private Neo4j neo4j;

	@Override
	public void start() {
		super.start();
		String neo4jAddress = container.config().getString("neo4j-address");
		if (neo4jAddress == null || neo4jAddress.trim().isEmpty()) {
			logger.fatal("Missing neo4j address.");
			return;
		}
		neo4j = new Neo4j(vertx.eventBus(), neo4jAddress);
		Validator.initLogin(neo4j);
		manual = new ManualFeeder(neo4j);
		vertx.eventBus().registerHandler(
				container.config().getString("address", "entcore.feeder"), this);
		switch (container.config().getString("feeder", "")) {
			case "AAF" :
				feed = new AafFeeder(vertx, container.config().getString("import-files"),
						container.config().getString("neo4j-aaf-extension-uri"));
				break;
			case "BE1D" :
				feed = new Be1dFeeder(vertx, container.config().getString("import-files"),
						container.config().getString("uai-separator","|"));
				break;
			default: throw new IllegalArgumentException("Invalid importer");
		}

	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		if (action.startsWith("manual-") && !Importer.getInstance().isReady()) {
			sendError(message, "concurrent.import");
		}
		switch (action) {
			case "manual-create-structure" : manual.createStructure(message);
				break;
			case "manual-create-class" : manual.createClass(message);
				break;
			case "manual-update-class" : manual.updateClass(message);
				break;
			case "manual-create-user" : manual.createUser(message);
				break;
			case "manual-update-user" : manual.updateUser(message);
				break;
			case "manual-csv-class-student" : manual.csvClassStudent(message);
				break;
			case "manual-csv-class-relative" : manual.csvClassRelative(message);
				break;
			case "import" : launchImport(message);
				break;
			default:
				sendError(message, "invalid.action");
		}
	}

	private void launchImport(final Message<JsonObject> message) {
		final Importer importer = Importer.getInstance();
		if (importer.isReady()) { // TODO else manage queue
			final long start = System.currentTimeMillis();
			importer.init(neo4j, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if (!"ok".equals(res.body().getString("status"))) {
						logger.error(res.body().getString("message"));
						return;
					}
					try {
						feed.launch(importer, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> m) {
								if (m != null && "ok".equals(m.body().getString("status"))) {
									logger.info(m.body().encode());
									if (config.getBoolean("apply-communication-rules", false)) {
										String q = "MATCH (s:Structure) return COLLECT(s.id) as ids";
										neo4j.execute(q, new JsonObject(), new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> message) {
												JsonArray ids = message.body().getArray("result", new JsonArray());
												if ("ok".equals(message.body().getString("status")) && ids != null &&
														ids.size() == 1) {
													JsonObject j = new JsonObject()
															.putString("action", "setMultipleDefaultCommunicationRules")
															.putArray("schoolIds", ((JsonObject) ids.get(0))
																	.getArray("ids", new JsonArray()));
													eb.send("wse.communication", j);
												} else {
													logger.error(message.body().getString("message"));
												}
										 }
										});
									}
									sendOK(message);
								} else if (m != null) {
									logger.error(m.body().getString("message"));
									sendError(message, m.body().getString("message"));
								} else {
									logger.error("Import return null value.");
									sendError(message, "Import return null value.");
								}
								logger.info("Elapsed time " + (System.currentTimeMillis() - start) + " ms.");
								importer.clear();
							}
						});
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						importer.clear();
					}
				}
			});
		}
	}

}
