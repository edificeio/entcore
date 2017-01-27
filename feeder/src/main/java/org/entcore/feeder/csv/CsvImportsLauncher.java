/*
 * Copyright © WebServices pour l'Éducation, 2017
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
 */

package org.entcore.feeder.csv;

import org.entcore.common.utils.FileUtils;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvImportsLauncher implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(CsvImportsLauncher.class);
	private static final Pattern UAI_PATTERN = Pattern.compile(".*([0-9]{7}[A-Z]).*");
	private final Pattern namePattern;
	private final Vertx vertx;
	private final String path;
	private final JsonObject profiles;
	private final boolean preDelete;
	private final PostImport postImport;

	public CsvImportsLauncher(Vertx vertx, String path, JsonObject config, PostImport postImport) {
		this.vertx = vertx;
		this.path = path;
		this.profiles = config.getObject("profiles");
		this.namePattern = Pattern.compile(config.getString("namePattern"));
		this.postImport = postImport;
		this.preDelete = config.getBoolean("preDelete", false);
	}

	@Override
	public void handle(Long event) {
		final FileSystem fs = vertx.fileSystem();
		fs.readDir(path, ".*.zip", new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(final AsyncResult<String[]> event) {
				if (event.succeeded()) {
					Arrays.sort(event.result());
					final VoidHandler[] handlers = new VoidHandler[event.result().length + 1];
					handlers[handlers.length - 1] = new VoidHandler() {
						@Override
						protected void handle() {
							postImport.execute();
						}
					};
					for (int i = event.result().length - 1; i >= 0; i--) {
						final int j = i;
						handlers[i] = new VoidHandler() {
							@Override
							protected void handle() {
								final String file = event.result()[j];
								log.info("Importing file : " + file);
								Matcher matcher;
								Matcher nameMatcher;
								if (file != null && (matcher = UAI_PATTERN.matcher(file)).find() &&
										(nameMatcher = namePattern.matcher(file)).find()) {
									final String uai = matcher.group(1);
									final String structureName = nameMatcher.group(1);
									TransactionManager.getNeo4jHelper()
											.execute("MATCH (s:User {UAI:{uai}}) return s.externalId as externalId",
													new JsonObject().putString("uai", uai), new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> event) {
											final String structureExternalId;
											JsonArray res = event.body().getArray("result");
											if ("ok".equals(event.body().getString("status")) && res.size() > 0) {
												structureExternalId = res.<JsonObject>get(0).getString("externalId");
											} else {
												structureExternalId = null;
											}
											final String parentDir = path + File.separator + UUID.randomUUID().toString();
											final String dirName = parentDir + File.separator + structureName +
													(structureExternalId != null ? "@" + structureExternalId : "") + "_" + uai;
											fs.mkdir(dirName, true, new Handler<AsyncResult<Void>>() {
												@Override
												public void handle(AsyncResult<Void> event) {
													try {
														FileUtils.unzip(file.replaceAll("\\s", "%20"), dirName);
																moveCsvFiles(structureExternalId, fs, dirName, parentDir, handlers, j);
													} catch (IOException e) {
														fs.delete(parentDir, true, null);
														log.error("Error unzip : " + file, e);
														handlers[j + 1].handle(null);
													}
												}
											});
										}
									});
								} else {
									log.error("UAI not found in filename : " + file);
									handlers[j + 1].handle(null);
								}
							}
						};
					}
					handlers[0].handle(null);
				} else {
					log.error("Error reading directory.");
				}
			}
		});
	}

	private void moveCsvFiles(final String structureExternalId, final FileSystem fs, final String dirName, final String parentDir,
			final VoidHandler[] handlers, final int j) {
		fs.readDir(dirName, ".*.csv", new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(final AsyncResult<String[]> l) {
				if (l.succeeded()) {
					final int size = l.result().length;
					if (!(size > 0)) {
						emptyDirectory(fs, parentDir, handlers, j);
						return;
					}

					final AtomicInteger validFilesCount = new AtomicInteger(size);
					final AtomicInteger count = new AtomicInteger(size);
					for (final String f : l.result()) {
						String profile = null;
						for (String profilePattern : profiles.getFieldNames()) {
							if (f.contains(profilePattern)) {
								profile = profiles.getString(profilePattern);
								break;
							}
						}
						if (profile == null) {
							validFilesCount.decrementAndGet();
							fs.delete(f, new Handler<AsyncResult<Void>>() {
								@Override
								public void handle(AsyncResult<Void> event) {
									if (count.decrementAndGet() == 0) {
										importFiles(validFilesCount, parentDir, structureExternalId, handlers, j, fs);
									}
								}
							});
							continue;
						}

						fs.move(f, dirName + File.separator + profile + ".csv", new Handler<AsyncResult<Void>>() {
							@Override
							public void handle(AsyncResult<Void> event2) {
								if (event2.succeeded()) {
									if (count.decrementAndGet() == 0) {
										importFiles(validFilesCount, parentDir, structureExternalId, handlers, j, fs);
									}
								} else {
									fs.delete(parentDir, true, null);
									log.error("Error mv csv file : " + f, l.cause());
									handlers[j + 1].handle(null);
								}
							}
						});
					}
				} else {
					fs.delete(parentDir, true, null);
					log.error("Error listing csv in directory : " + dirName, l.cause());
					handlers[j + 1].handle(null);
				}
			}
		});
	}

	private void importFiles(AtomicInteger validFilesCount, final String dirName, String structureExternalId,
			final VoidHandler[] handlers, final int j, final FileSystem fs) {
		if (validFilesCount.get() > 0) {
			JsonObject action = new JsonObject()
					.putString("action", "import")
					.putString("feeder", "CSV")
					.putString("path", dirName)
					.putString("structureExternalId", structureExternalId)
					.putBoolean("postImport", false)
					.putBoolean("preDelete", preDelete);
			vertx.eventBus().send("entcore.feeder", action, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					log.info(event.body().encodePrettily());
					fs.delete(dirName, true, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							handlers[j + 1].handle(null);
						}
					});
				}
			});
		} else {
			emptyDirectory(fs, dirName, handlers, j);
		}
	}

	private void emptyDirectory(FileSystem fs, String dirName, VoidHandler[] handlers, int j) {
		fs.delete(dirName, true, null);
		log.error("Empty directory : " + dirName);
		handlers[j + 1].handle(null);
	}

}
