/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.feeder.export.eliot;

import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.dictionary.structures.Tenant;
import org.entcore.feeder.export.Exporter;
import org.entcore.feeder.utils.Function;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EliotExporter implements Exporter {

	public static final String WEBDAV_ADDRESS = "webdav";
	private static final Logger log = LoggerFactory.getLogger(EliotExporter.class);
	public static final String ELIOT = "ELIOT";
	private final String exportBasePath;
	private final String exportDestination;
	private final Vertx vertx;
	private static final DateFormat datetime = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final DateFormat date = new SimpleDateFormat("yyyyMMdd");
	private final String node;
	private final boolean concatFiles;
	private final boolean deleteExport;

	public EliotExporter(String exportPath, String exportDestination, boolean concatFiles, boolean deleteExport, Vertx vertx) {
		this.exportBasePath = exportPath;
		this.exportDestination = exportDestination;
		this.vertx = vertx;
		String n = (String) vertx.sharedData().getMap("server").get("node");
		this.node = (n != null) ? n : "";
		this.concatFiles = concatFiles;
		this.deleteExport = deleteExport;
	}

	@Override
	public void export(final Handler<Message<JsonObject>> handler) throws Exception {
		TransactionManager.executeTransaction(new Function<TransactionHelper, Message<JsonObject>>() {
			@Override
			public void apply(TransactionHelper value) {
				Tenant.list(new JsonArray().add("name").add("academy"), null, null, value);
				Structure.list(ELIOT, new JsonArray().add("academy"), null, null, value);
			}

			@Override
			public void handle(Message<JsonObject> result) {
				JsonArray r = result.body().getArray("results");
				if ("ok".equals(result.body().getString("status")) && r != null && r.size() == 2) {
					final String tenant = r.<JsonArray>get(0).<JsonObject>get(0).getString("name");
					final String academy = r.<JsonArray>get(0).<JsonObject>get(0).getString("academy", r.<JsonArray>get(1).<JsonObject>get(0).getString("academy"));
					final Date exportDate = new Date();
					final String path = exportBasePath + File.separator +
							tenant + "_Complet_" + datetime.format(exportDate) + "_Export";
					log.info("Export path " + path);
					vertx.fileSystem().mkdir(path, true, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> ar) {
							if (ar.succeeded()) {
								new EleveExportProcessing(path, date.format(exportDate), tenant + "_" + academy, concatFiles)
										.start(new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> message) {
												if ("ok".equals(message.body().getString("status"))) {
													if (exportDestination != null && !exportDestination.trim().isEmpty()) {
														zipAndSend(path, handler);
													} else {
														log.warn("export not send");
														message.body().putString("exportPath", path);
														handler.handle(message);
													}
												} else {
													log.error(message.body().encode());
													handler.handle(message);
												}
											}
										});
							} else {
								log.error(ar.cause().getMessage(), ar.cause());
								handler.handle(new ResultMessage().error(ar.cause().getMessage()));
							}
						}
					});
				} else {
					log.error(result.body().encode());
					handler.handle(result);
				}
			}
		});
	}

	@Override
	public String getName() {
		return ELIOT;
	}

	private void zipAndSend(final String path, final Handler<Message<JsonObject>> handler) {
		final String zipPath = path + ".zip";
		log.info("Export zip : " + zipPath);
		vertx.fileSystem().readDir(path, new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(AsyncResult<String[]> asyncResult) {
				if (asyncResult.succeeded()) {
					JsonObject j = new JsonObject()
							.putArray("path", new JsonArray(asyncResult.result()))
							.putString("zipFile", zipPath)
							.putBoolean("deletePath", true);
					vertx.eventBus().send(node + "entcore.zipper", j, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								sendWithWebDav(zipPath, handler);
								vertx.fileSystem().delete(path, true, null);
							} else {
								log.error("Error zipping export : ");
								log.error(event.body().encode());
								handler.handle(event);
							}
						}
					});
				} else {
					log.error(asyncResult.cause().getMessage(), asyncResult.cause());
					handler.handle(new ResultMessage().error(asyncResult.cause().getMessage()));
				}
			}
		});
	}

	private void sendWithWebDav(final String file, final Handler<Message<JsonObject>> handler) {
		final EventBus eb = vertx.eventBus();
		JsonObject j = new JsonObject()
				.putString("action", "put")
				.putString("uri", exportDestination +
						file.substring(file.lastIndexOf(File.separator) + 1))
				.putString("file", file);
		eb.send(node + WEBDAV_ADDRESS, j, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				if ("ok".equals(message.body().getString("status"))) {
					if (deleteExport) {
						vertx.fileSystem().delete(file, null);
					}
				} else {
					log.error("Error sending export : ");
					log.error(message.body().encode());
				}
				handler.handle(message);
		  }
		});
	}

}
