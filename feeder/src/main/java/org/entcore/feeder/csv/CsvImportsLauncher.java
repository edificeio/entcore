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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

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
		this.profiles = config.getJsonObject("profiles");
		this.namePattern = Pattern.compile(config.getString("namePattern"));
		this.postImport = postImport;
		this.preDelete = config.getBoolean("preDelete", false);
	}

	@Override
	public void handle(Long event) {
		final FileSystem fs = vertx.fileSystem();
		fs.readDir(path, ".*.zip", new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(final AsyncResult<List<String>> event) {
				if (event.succeeded()) {
					Collections.sort(event.result());
					final Handler[] handlers = new Handler[event.result().size() + 1];
					handlers[handlers.length - 1] = new Handler<Void>() {
						@Override
						public void handle(Void v) {
							postImport.execute();
						}
					};
					for (int i = event.result().size() - 1; i >= 0; i--) {
						final int j = i;
						handlers[i] = new Handler<Void>() {
							@Override
							public void handle(Void v) {
								final String file = event.result().get(j);
								log.info("Importing file : " + file);
								Matcher matcher;
								Matcher nameMatcher;
								if (file != null && (matcher = UAI_PATTERN.matcher(file)).find() &&
										(nameMatcher = namePattern.matcher(file)).find()) {
									final String uai = matcher.group(1);
									final String structureName = nameMatcher.group(1);
									TransactionManager.getNeo4jHelper()
											.execute("MATCH (s:Structure {UAI:{uai}}) return s.externalId as externalId",
													new JsonObject().put("uai", uai), new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> event) {
											final String structureExternalId;
											JsonArray res = event.body().getJsonArray("result");
											if ("ok".equals(event.body().getString("status")) && res.size() > 0) {
												structureExternalId = res.getJsonObject(0).getString("externalId");
											} else {
												structureExternalId = null;
											}
											final String parentDir = path + File.separator + UUID.randomUUID().toString();
											final String dirName = parentDir + File.separator + structureName +
													(structureExternalId != null ? "@" + structureExternalId : "") + "_" + uai;
											fs.mkdirs(dirName, new Handler<AsyncResult<Void>>() {
												@Override
												public void handle(AsyncResult<Void> event) {
													try {
														FileUtils.unzip(file.replaceAll("\\s", "%20"), dirName);
																moveCsvFiles(structureExternalId, fs, dirName, parentDir, handlers, j);
													} catch (IOException e) {
														fs.deleteRecursive(parentDir, true, null);
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
			final Handler<Void>[] handlers, final int j) {
		fs.readDir(dirName, ".*.csv", new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(final AsyncResult<List<String>> l) {
				if (l.succeeded()) {
					final int size = l.result().size();
					if (!(size > 0)) {
						emptyDirectory(fs, parentDir, handlers, j);
						return;
					}

					final AtomicInteger validFilesCount = new AtomicInteger(size);
					final AtomicInteger count = new AtomicInteger(size);
					for (final String f : l.result()) {
						String profile = null;
						for (String profilePattern : profiles.fieldNames()) {
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
									fs.deleteRecursive(parentDir, true, null);
									log.error("Error mv csv file : " + f, l.cause());
									handlers[j + 1].handle(null);
								}
							}
						});
					}
				} else {
					fs.deleteRecursive(parentDir, true, null);
					log.error("Error listing csv in directory : " + dirName, l.cause());
					handlers[j + 1].handle(null);
				}
			}
		});
	}

	private void importFiles(AtomicInteger validFilesCount, final String dirName, String structureExternalId,
			final Handler<Void>[] handlers, final int j, final FileSystem fs) {
		if (validFilesCount.get() > 0) {
			JsonObject action = new JsonObject()
					.put("action", "import")
					.put("feeder", "CSV")
					.put("path", dirName)
					.put("structureExternalId", structureExternalId)
					.put("postImport", false)
					.put("preDelete", preDelete);
			vertx.eventBus().send("entcore.feeder", action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					log.info(event.body().encodePrettily());
					fs.deleteRecursive(dirName, true, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							handlers[j + 1].handle(null);
						}
					});
				}
			}));
		} else {
			emptyDirectory(fs, dirName, handlers, j);
		}
	}

	private void emptyDirectory(FileSystem fs, String dirName, Handler<Void>[] handlers, int j) {
		fs.deleteRecursive(dirName, true, null);
		log.error("Empty directory : " + dirName);
		handlers[j + 1].handle(null);
	}

}
