/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.feeder;

import fr.wseduc.cron.CronTrigger;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.aaf1d.Aaf1dFeeder;
import org.entcore.feeder.be1d.Be1dFeeder;
import org.entcore.feeder.csv.CsvFeeder;
import org.entcore.feeder.dictionary.structures.*;
import org.entcore.feeder.export.Exporter;
import org.entcore.feeder.export.eliot.EliotExporter;
import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class Feeder extends BusModBase implements Handler<Message<JsonObject>> {

	public static final String USER_REPOSITORY = "user.repository";
	public static final String FEEDER_ADDRESS = "entcore.feeder";
	private String defaultFeed;
	private final Map<String, Feed> feeds = new HashMap<>();
	private ManualFeeder manual;
	private Neo4j neo4j;
	private Exporter exporter;
	private EventStore eventStore;
	private DuplicateUsers duplicateUsers;
	public enum FeederEvent { IMPORT, DELETE_USER, CREATE_USER }

	@Override
	public void start() {
		super.start();
		String neo4jAddress = container.config().getString("neo4j-address");
		if (neo4jAddress == null || neo4jAddress.trim().isEmpty()) {
			logger.fatal("Missing neo4j address.");
			return;
		}
		String node = (String) vertx.sharedData().getMap("server").get("node");
		if (node == null) {
			node = "";
		}
		neo4j = new Neo4j(vertx.eventBus(), node + neo4jAddress);
		TransactionManager.getInstance().setNeo4j(neo4j);
		EventStoreFactory factory = EventStoreFactory.getFactory();
		factory.setVertx(vertx);
		eventStore = factory.getEventStore(Feeder.class.getSimpleName());
		final long deleteUserDelay = container.config().getLong("delete-user-delay", 90 * 24 * 3600 * 1000l);
		final long preDeleteUserDelay = container.config().getLong("pre-delete-user-delay", 90 * 24 * 3600 * 1000l);
		final String deleteCron = container.config().getString("delete-cron", "0 0 2 * * ? *");
		final String preDeleteCron = container.config().getString("pre-delete-cron", "0 0 3 * * ? *");
		final String importCron = container.config().getString("import-cron");
		try {
			new CronTrigger(vertx, deleteCron).schedule(new User.DeleteTask(deleteUserDelay, eb));
			new CronTrigger(vertx, preDeleteCron).schedule(new User.PreDeleteTask(preDeleteUserDelay));
			if (importCron != null && !importCron.trim().isEmpty()) {
				new CronTrigger(vertx, importCron).schedule(new ImporterTask(eb,
						container.config().getBoolean("auto-export", false)));
			}
		} catch (ParseException e) {
			logger.fatal(e.getMessage(), e);
			vertx.stop();
			return;
		}
		Validator.initLogin(neo4j);
		manual = new ManualFeeder(neo4j);
		duplicateUsers = new DuplicateUsers(container.config().getArray("duplicateSources"));
		vertx.eventBus().registerLocalHandler(
				container.config().getString("address", FEEDER_ADDRESS), this);
		defaultFeed = container.config().getString("feeder", "AAF");
		feeds.put("AAF", new AafFeeder(vertx, container.config().getString("import-files"),
				container.config().getBoolean("aafNeo4jPlugin", false)));
		feeds.put("AAF1D", new Aaf1dFeeder(vertx, container.config().getString("import-files")));
		feeds.put("BE1D", new Be1dFeeder(vertx, container.config().getString("import-files"),
				container.config().getString("uai-separator", "_")));
		feeds.put("CSV", new CsvFeeder(container.config().getObject("csvMappings", new JsonObject())));
		switch (container.config().getString("exporter", "")) {
			case "ELIOT" :
				exporter = new EliotExporter(container.config().getString("export-path", "/tmp"),
						container.config().getString("export-destination"),
						container.config().getBoolean("concat-export", true), vertx);
				break;
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
			case "manual-update-structure" : manual.updateStructure(message);
				break;
			case "manual-create-class" : manual.createClass(message);
				break;
			case "manual-update-class" : manual.updateClass(message);
				break;
			case "manual-create-user" : manual.createUser(message);
				break;
			case "manual-update-user" : manual.updateUser(message);
				break;
			case "manual-add-user" : manual.addUser(message);
				break;
			case "manual-remove-user" : manual.removeUser(message);
				break;
			case "manual-delete-user" : manual.deleteUser(message);
				break;
			case "manual-restore-user" : manual.restoreUser(message);
				break;
			case "manual-create-function" : manual.createFunction(message);
				break;
			case "manual-delete-function" : manual.deleteFunction(message);
				break;
			case "manual-create-function-group" : manual.createFunctionGroup(message);
				break;
			case "manual-delete-function-group" : manual.deleteFunctionGroup(message);
				break;
			case "manual-create-group" : manual.createGroup(message);
				break;
			case "manual-delete-group" : manual.deleteGroup(message);
				break;
			case "manual-relative-student" : manual.relativeStudent(message);
				break;
			case "manual-unlink-relative-student" : manual.unlinkRelativeStudent(message);
			break;
			case "manual-add-user-function" : manual.addUserFunction(message);
				break;
			case "manual-remove-user-function" : manual.removeUserFunction(message);
				break;
			case "manual-add-user-group" : manual.addUserGroup(message);
				break;
			case "manual-remove-user-group" : manual.removeUserGroup(message);
				break;
			case "manual-create-tenant" : manual.createOrUpdateTenant(message);
				break;
			case "manual-csv-class-student" : manual.csvClassStudent(message);
				break;
			case "manual-csv-class-relative" : manual.csvClassRelative(message);
				break;
			case "manual-structure-attachment" : manual.structureAttachment(message);
				break;
			case "manual-structure-detachment" : manual.structureDetachment(message);
				break;
			case "transition" : launchTransition(message);
				break;
			case "import" : launchImport(message);
				break;
			case "export" : launchExport(message);
				break;
			case "ignore-duplicate" :
				duplicateUsers.ignoreDuplicate(message);
				break;
			case "list-duplicate" :
				duplicateUsers.listDuplicates(message);
				break;
			case "merge-duplicate" :
				duplicateUsers.mergeDuplicate(message);
				break;
			case "mark-duplicates" :
				duplicateUsers.markDuplicates(message);
				break;
			default:
				sendError(message, "invalid.action");
		}
	}

	private void launchExport(final Message<JsonObject> message) {
		if (exporter == null) {
			sendError(message, "exporter.not.found");
			return;
		}
		try {
			final long start = System.currentTimeMillis();
			exporter.export(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					logger.info("Elapsed time " + (System.currentTimeMillis() - start) + " ms.");
					logger.info(m.body().encode());
					message.reply(m.body());
					eb.publish(USER_REPOSITORY, new JsonObject()
							.putString("action", "exported")
							.putString("exportFormat", exporter.getName())
					);
				}
			});
		} catch (Exception e) {
			sendError(message, e.getMessage(), e);
		}
	}

	private void launchTransition(final Message<JsonObject> message) {
		if (GraphData.isReady()) { // TODO else manage queue
			String structureExternalId = message.body().getString("structureExternalId");
			Transition transition = new Transition();
			transition.launch(structureExternalId, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					if (m != null && "ok".equals(m.body().getString("status"))) {
						logger.info("Delete groups : " + m.body().encode());
						eb.publish(USER_REPOSITORY, new JsonObject()
								.putString("action", "delete-groups")
								.putArray("old-groups", m.body().getArray("result", new JsonArray())));
						sendOK(message, m.body());
					} else if (m != null) {
						logger.error(m.body().getString("message"));
						sendError(message, m.body().getString("message"));
					} else {
						logger.error("Transition return null value.");
						sendError(message, "Transition return null value.");
					}
					GraphData.clear();
				}
			});

		}
	}

	private void launchImport(final Message<JsonObject> message) {
		final Importer importer = Importer.getInstance();
		final Feed feed = feeds.get(message.body().getString("feeder", defaultFeed));
		final String profile = message.body().getString("profile");
		final String content = message.body().getString("content");
		final String structureExternalId = message.body().getString("structureExternalId");
		final String charset = message.body().getString("charset", "UTF-8");
		if (feed == null) {
			sendError(message, "invalid.feeder");
			return;
		}
		if (importer.isReady()) { // TODO else manage queue
			final long start = System.currentTimeMillis();
			importer.init(neo4j, feed.getSource(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if (!"ok".equals(res.body().getString("status"))) {
						logger.error(res.body().getString("message"));
						return;
					}
					try {
						Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> m) {
								if (m != null && "ok".equals(m.body().getString("status"))) {
									logger.info(m.body().encode());
									storeImportedEvent();
									duplicateUsers.markDuplicates(new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject event) {
											applyComRules(new VoidHandler() {
												@Override
												protected void handle() {
													if (config.getBoolean("notify-apps-after-import", true)) {
														ApplicationUtils.afterImport(eb);
													}
												}
											});
										}
									});
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
						};
						if (profile != null && content != null && structureExternalId != null &&
								!content.trim().isEmpty() && ManualFeeder.profiles.containsKey(profile) &&
								!structureExternalId.trim().isEmpty() && feed instanceof PartialFeed) {
							((PartialFeed) feed).launch(profile, structureExternalId, content, charset, importer, handler);
						} else {
							feed.launch(importer, handler);
						}
					} catch (Exception e) {
						importer.clear();
						sendError(message, e.getMessage(), e);
					}
				}
			});
		}
	}

	private void storeImportedEvent() {
		String countQuery =
				"MATCH (:User) WITH count(*) as nbUsers " +
				"MATCH (:Structure) WITH count(*) as nbStructures, nbUsers " +
				"MATCH (:Class) RETURN nbUsers, nbStructures, count(*) as nbClasses ";
		neo4j.execute(countQuery, null, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					eventStore.createAndStoreEvent(FeederEvent.IMPORT.name(),
							(UserInfos) null, res.<JsonObject>get(0));
				} else {
					logger.error(event.body().getString("message"));
				}
			}
		});
	}

	private void applyComRules(final VoidHandler handler) {
		if (config.getBoolean("apply-communication-rules", false)) {
			String q = "MATCH (s:Structure) return COLLECT(s.id) as ids";
			neo4j.execute(q, new JsonObject(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					JsonArray ids = message.body().getArray("result", new JsonArray());
					if ("ok".equals(message.body().getString("status")) && ids != null &&
							ids.size() == 1) {
						JsonObject j = new JsonObject()
								.putString("action", "initAndApplyDefaultCommunicationRules")
								.putArray("schoolIds", ((JsonObject) ids.get(0))
										.getArray("ids", new JsonArray()));
						eb.send("wse.communication", j, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									logger.error("Init rules error : " + event.body().getString("message"));
								} else {
									logger.info("Communication rules applied.");
								}
								handler.handle(null);
							}
						});
					} else {
						logger.error(message.body().getString("message"));
						handler.handle(null);
					}
			 }
			});
		} else {
			handler.handle(null);
		}
	}

}
