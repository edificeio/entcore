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
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.aaf1d.Aaf1dFeeder;
import org.entcore.feeder.csv.CsvFeeder;
import org.entcore.feeder.csv.CsvImportsLauncher;
import org.entcore.feeder.csv.CsvValidator;
import org.entcore.feeder.dictionary.structures.*;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.timetable.ImportsLauncher;
import org.entcore.feeder.timetable.edt.EDTImporter;
import org.entcore.feeder.timetable.edt.EDTUtils;
import org.entcore.feeder.export.Exporter;
import org.entcore.feeder.export.eliot.EliotExporter;
import org.entcore.feeder.timetable.udt.UDTImporter;
import org.entcore.feeder.utils.*;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Feeder extends BusModBase implements Handler<Message<JsonObject>> {

	public static final String USER_REPOSITORY = "user.repository";
	public static final String FEEDER_ADDRESS = "entcore.feeder";
	private String defaultFeed;
	private final Map<String, Feed> feeds = new HashMap<>();
	private ManualFeeder manual;
	private Neo4j neo4j;
	private Exporter exporter;
	private DuplicateUsers duplicateUsers;
	private PostImport postImport;
	private final ConcurrentLinkedQueue<Message<JsonObject>> eventQueue = new ConcurrentLinkedQueue<>();
	public enum FeederEvent { IMPORT, DELETE_USER, CREATE_USER }
	private EDTUtils edtUtils;

	@Override
	public void start() {
		super.start();
		String node = (String) vertx.sharedData().getMap("server").get("node");
		if (node == null) {
			node = "";
		}
		String neo4jConfig = (String) vertx.sharedData().getMap("server").get("neo4jConfig");
		if (neo4jConfig != null) {
			neo4j = Neo4j.getInstance();
			neo4j.init(vertx, new JsonObject(neo4jConfig));
		}
		MongoDb.getInstance().init(vertx.eventBus(), node + "wse.mongodb.persistor");
		TransactionManager.getInstance().setNeo4j(neo4j);
		EventStoreFactory.getFactory().setVertx(vertx);
		defaultFeed = container.config().getString("feeder", "AAF");
		feeds.put("AAF", new AafFeeder(vertx, getFilesDirectory("AAF")));
		feeds.put("AAF1D", new Aaf1dFeeder(vertx, getFilesDirectory("AAF1D")));
		feeds.put("CSV", new CsvFeeder(vertx, container.config().getObject("csvMappings", new JsonObject())));
		final long deleteUserDelay = container.config().getLong("delete-user-delay", 90 * 24 * 3600 * 1000l);
		final long preDeleteUserDelay = container.config().getLong("pre-delete-user-delay", 90 * 24 * 3600 * 1000l);
		final String deleteCron = container.config().getString("delete-cron", "0 0 2 * * ? *");
		final String preDeleteCron = container.config().getString("pre-delete-cron", "0 0 3 * * ? *");
		final String importCron = container.config().getString("import-cron");
		final JsonObject imports = container.config().getObject("imports");
		final JsonObject preDelete = container.config().getObject("pre-delete");
		try {
			new CronTrigger(vertx, deleteCron).schedule(new User.DeleteTask(deleteUserDelay, eb));
			if (preDelete != null) {
				if (preDelete.size() == ManualFeeder.profiles.size() &&
						ManualFeeder.profiles.keySet().containsAll(preDelete.getFieldNames())) {
					for (String profile : preDelete.getFieldNames()) {
						final JsonObject profilePreDelete = preDelete.getObject(profile);
						if (profilePreDelete == null || profilePreDelete.getString("cron") == null ||
								profilePreDelete.getLong("delay") == null) continue;
						new CronTrigger(vertx, profilePreDelete.getString("cron"))
								.schedule(new User.PreDeleteTask(profilePreDelete.getLong("delay"), profile));
					}
				}
			} else {
				new CronTrigger(vertx, preDeleteCron).schedule(new User.PreDeleteTask(preDeleteUserDelay));
			}
			if (imports != null) {
				if (feeds.keySet().containsAll(imports.getFieldNames())) {
					for (String f : imports.getFieldNames()) {
						final JsonObject i = imports.getObject(f);
						if (i != null && i.getString("cron") != null) {
							new CronTrigger(vertx, i.getString("cron")).schedule(
									new ImporterTask(eb, f, i.getBoolean("auto-export", false)));
						}
					}
				} else {
					logger.error("Invalid imports configuration.");
				}
			} else if (importCron != null && !importCron.trim().isEmpty()) {
				new CronTrigger(vertx, importCron).schedule(new ImporterTask(eb, defaultFeed,
						container.config().getBoolean("auto-export", false)));
			}
		} catch (ParseException e) {
			logger.fatal(e.getMessage(), e);
			vertx.stop();
			return;
		}
		Validator.initLogin(neo4j, vertx);
		manual = new ManualFeeder(neo4j);
		duplicateUsers = new DuplicateUsers(container.config().getBoolean("timetable", true),
				container.config().getBoolean("autoMergeOnlyInSameStructure", true));
		postImport = new PostImport(vertx, duplicateUsers, container.config());
		vertx.eventBus().registerLocalHandler(
				container.config().getString("address", FEEDER_ADDRESS), this);
		switch (container.config().getString("exporter", "")) {
			case "ELIOT" :
				exporter = new EliotExporter(container.config().getString("export-path", "/tmp"),
						container.config().getString("export-destination"),
						container.config().getBoolean("concat-export", false),
						container.config().getBoolean("delete-export", true), vertx);
				break;
		}
		final JsonObject edt = container.config().getObject("edt");
		if (edt != null) {
			final String pronotePrivateKey = edt.getString("pronote-private-key");
			if (isNotEmpty(pronotePrivateKey)) {
				edtUtils = new EDTUtils(vertx, pronotePrivateKey,
						container.config().getString("pronote-partner-name", "NEO-Open"));
				final String edtPath = edt.getString("path");
				final String edtCron = edt.getString("cron");
				if (isNotEmpty(edtPath) && isNotEmpty(edtCron)) {
					try {
						new CronTrigger(vertx, edtCron).schedule(
								new ImportsLauncher(vertx, edtPath, postImport, edtUtils));
					} catch (ParseException e) {
						logger.error("Error in cron edt", e);
					}
				}
			}
		}
		final JsonObject udt = container.config().getObject("udt");
		if (udt != null) {
			final String udtPath = udt.getString("path");
			final String udtCron = udt.getString("cron");
			if (isNotEmpty(udtPath) && isNotEmpty(udtCron)) {
				try {
					new CronTrigger(vertx, udtCron).schedule(
							new ImportsLauncher(vertx, udtPath, postImport, edtUtils));
				} catch (ParseException e) {
					logger.error("Error in cron udt", e);
				}
			}
		}
		final JsonObject csv = container.config().getObject("csv");
		if (csv != null) {
			final String csvPath = csv.getString("path");
			final String csvCron = csv.getString("cron");
			final JsonObject csvConfig = csv.getObject("config");
			if (isNotEmpty(csvPath) && isNotEmpty(csvCron) && csvConfig != null) {
				try {
					new CronTrigger(vertx, csvCron).schedule(
							new CsvImportsLauncher(vertx, csvPath, csvConfig, postImport));
				} catch (ParseException e) {
					logger.error("Error in cron csv", e);
				}
			}
		}
		I18n.getInstance().init(container, vertx);
	}

	private String getFilesDirectory(String feeder) {
		JsonObject imports = container.config().getObject("imports");
		if (imports != null && imports.getObject(feeder) != null && imports.getObject(feeder).getString("files") != null) {
			return imports.getObject(feeder).getString("files");
		}
		return config.getString("import-files");
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		if (action.startsWith("manual-") && !Importer.getInstance().isReady()) {
			eventQueue.add(message);
			return;
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
			case "manual-structure-attachment" : manual.structureAttachment(message);
				break;
			case "manual-structure-detachment" : manual.structureDetachment(message);
				break;
			case "transition" : launchTransition(message, null);
				break;
			case "import" : launchImport(message);
				break;
			case "export" : launchExport(message);
				break;
			case "validate" : launchImportValidation(message, null);
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
			case "merge-by-keys" :
				duplicateUsers.mergeBykeys(message);
				break;
			case "mark-duplicates" :
				duplicateUsers.markDuplicates(message);
				break;
			case "automerge-duplicates" :
				duplicateUsers.autoMergeDuplicatesInStructure(new AsyncResultHandler<JsonArray>() {
					@Override
					public void handle(AsyncResult<JsonArray> event) {
						logger.info("auto merged : " + event.succeeded());
					}
				});
				break;
			case "manual-init-timetable-structure" :
				AbstractTimetableImporter.initStructure(eb, message);
				break;
			case "manual-edt":
				EDTImporter.launchImport(edtUtils, container.config().getString("mode", "prod"), message, postImport);
				break;
			case "manual-udt":
				UDTImporter.launchImport(vertx, message, postImport);
				break;
			case "reinit-logins" :
				Validator.initLogin(neo4j, vertx);
				break;
			default:
				sendError(message, "invalid.action");
		}
		checkEventQueue();
	}

	private void launchImportValidation(final Message<JsonObject> message, final Handler<Report> handler) {
		logger.info(message.body().encodePrettily());
		final String acceptLanguage = message.body().getString("language", "fr");
		final String source = message.body().getString("feeder", defaultFeed);

		// TODO make validator factory
		final ImportValidator v;
		switch (source) {
			case "CSV":
				v = new CsvValidator(vertx, acceptLanguage,
						container.config().getObject("csvMappings", new JsonObject()));
				break;
			case "AAF":
			case "AAF1D":
				final Report report = new Report(acceptLanguage);
				if (handler != null) {
					handler.handle(report);
				} else {
					sendOK(message, new JsonObject().putObject("result", report.getResult()));
				}
				return;
			default:
				sendError(message, "invalid.type");
				return;
		}

		final String structureExternalId = message.body().getString("structureExternalId");
		final boolean preDelete = message.body().getBoolean("preDelete", false);
		String path = message.body().getString("path");
		if (path == null && !"CSV".equals(source)) {
			path = container.config().getString("import-files");
		}
		v.validate(path, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject result) {
				final Report r = (Report) v;
				if (preDelete && structureExternalId != null && !r.containsErrors()) {
					final JsonArray externalIds = r.getUsersExternalId();
					final JsonArray profiles = r.getResult().getArray(Report.PROFILES);
					new User.PreDeleteTask(0).findMissingUsersInStructure(
							structureExternalId, source, externalIds, profiles, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							final JsonArray res = event.body().getArray("result");
							if ("ok".equals(event.body().getString("status")) && res != null) {
								for (Object o : res) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject j = (JsonObject) o;
									String filename = j.getString("profile");
									r.addUser(filename, j.putString("state", r.translate(Report.State.DELETED.name()))
											.putString("translatedProfile", r.translate(j.getString("profile"))));
								}
								r.getResult().putArray("usersExternalIds", externalIds);
							} else {
								r.addError("error.find.preDelete");
							}
							if (handler != null) {
								handler.handle(r);
							} else {
								sendOK(message, new JsonObject().putObject("result", r.getResult()));
							}
						}
					});
				} else {
					if (handler != null) {
						handler.handle(r);
					} else {
						sendOK(message, new JsonObject().putObject("result", r.getResult()));
					}
				}
			}
		});
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

	private void launchTransition(final Message<JsonObject> message, final Handler<Message<JsonObject>> handler) {
		if (GraphData.isReady()) {
			final String structureExternalId = message.body().getString("structureExternalId");
			Transition transition = new Transition();
			transition.launch(structureExternalId, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					if (m != null && "ok".equals(m.body().getString("status"))) {
						Transition.publishDeleteGroups(eb, logger, m.body().getArray("result", new JsonArray()));
						AbstractTimetableImporter.transition(structureExternalId);
						if (handler != null) {
							handler.handle(m);
						} else {
							sendOK(message, m.body());
						}
					} else if (m != null) {
						logger.error(m.body().getString("message"));
						if (handler != null) {
							handler.handle(m);
						} else {
							sendError(message, m.body().getString("message"));
						}
					} else {
						logger.error("Transition return null value.");
						if (handler != null) {
							handler.handle(new ResultMessage().error("transition.error"));
						} else {
							sendError(message, "Transition return null value.");
						}
					}
					GraphData.clear();
					checkEventQueue();
				}
			});
		} else {
			eventQueue.add(message);
		}
	}

	private void launchImport(final Message<JsonObject> message) {
		final String source = message.body().getString("feeder", defaultFeed);
		final Feed feed = feeds.get(source);
		if (feed == null) {
			sendError(message, "invalid.feeder");
			return;
		}

		final boolean preDelete = message.body().getBoolean("preDelete", false);
		final String structureExternalId = message.body().getString("structureExternalId");

		if (message.body().getBoolean("transition", false)) {
			launchTransition(message, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						validateAndImport(message, feed, preDelete, structureExternalId, source);
					} else {
						sendError(message, "transition.error");
					}
				}
			});
		} else {
			validateAndImport(message, feed, preDelete, structureExternalId, source);
		}
	}

	private void validateAndImport(final Message<JsonObject> message, final Feed feed, final boolean preDelete,
			final String structureExternalId, final String source) {
		launchImportValidation(message, new Handler<Report>() {
			@Override
			public void handle(final Report report) {
				if (report != null && !report.containsErrors()) {
					doImport(message, feed, new Handler<Report>() {
						@Override
						public void handle(final Report importReport) {
							if (importReport == null) {
								sendError(message, "import.error");
								return;
							}
							final JsonObject ir = importReport.getResult();
							final JsonArray existingUsers = ir.getArray("usersExternalIds");
							final JsonArray profiles = ir.getArray(Report.PROFILES);
							if (preDelete && structureExternalId != null && existingUsers != null &&
									existingUsers.size() > 0 && !importReport.containsErrors()) {
								new User.PreDeleteTask(0).preDeleteMissingUsersInStructure(
										structureExternalId, source, existingUsers, profiles, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (!"ok".equals(event.body().getString("status"))) {
											importReport.addError("preDelete.error");
										}
										sendOK(message, new JsonObject().putObject("result", importReport.getResult()));
									}
								});
							} else {
								sendOK(message, new JsonObject().putObject("result", importReport.getResult()));
							}
						}
					});
				} else if (report != null) {
					sendOK(message, new JsonObject().putObject("result", report.getResult()));
				} else {
					sendError(message, "validation.error");
				}
			}
		});
	}

	private void doImport(final Message<JsonObject> message, final Feed feed, final Handler<Report> h) {

		final String acceptLanguage = message.body().getString("language", "fr");


		final String charset = message.body().getString("charset", "UTF-8");
		final String importPath = message.body().getString("path");
		final boolean executePostImport = message.body().getBoolean("postImport", true);

		final Importer importer = Importer.getInstance();
		if (importer.isReady()) {
			final long start = System.currentTimeMillis();
			importer.init(neo4j, feed.getSource(), acceptLanguage, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if (!"ok".equals(res.body().getString("status"))) {
						logger.error(res.body().getString("message"));
						h.handle(new Report(acceptLanguage).addError("init.importer.error"));
						importer.clear();
						checkEventQueue();
						return;
					}
					final Report report = importer.getReport();
					try {
						Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> m) {
								if (m != null && "ok".equals(m.body().getString("status"))) {
									logger.info(m.body().encode());
									if (executePostImport) {
										postImport.execute(feed.getSource());
									}
								} else {
									Validator.initLogin(neo4j, vertx);
									if (m != null) {
										logger.error(m.body().getString("message"));
										report.addError(m.body().getString("message"));
									} else if (report.getResult().getObject("errors").size() < 1) {
										logger.error("Import return null value.");
										report.addError("import.error");
									}
								}
								report.setUsersExternalId(new JsonArray(importer.getUserImportedExternalId().toArray()));
								h.handle(report);
								final long endTime = System.currentTimeMillis();
								report.setEndTime(endTime);
								report.setStartTime(start);
								report.countDiff(new VoidHandler() {
									@Override
									protected void handle() {
										report.emailReport(vertx, container);
									}
								});
								logger.info("Elapsed time " + (endTime - start) + " ms.");
								importer.clear();
								checkEventQueue();
							}
						};
						if (importPath != null && !importPath.trim().isEmpty()) {
							feed.launch(importer, importPath, handler);
						} else {
							feed.launch(importer, handler);
						}
					} catch (Exception e) {
						Validator.initLogin(neo4j, vertx);
						importer.clear();
						h.handle(report.addError("import.error"));
						logger.error(e.getMessage(), e);
						checkEventQueue();
					}
				}
			});
		} else {
			eventQueue.add(message);
		}
	}

	private void checkEventQueue() {
		Message<JsonObject> event = eventQueue.poll();
		if (event != null) {
			switch (event.body().getString("action", "")) {
				case "import": launchImport(event);
					break;
				case "transition": launchTransition(event, null);
					break;
				default:
					handle(event);
			}
		}
	}

}
