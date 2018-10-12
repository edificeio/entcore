/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.feeder.export.eliot;

import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.dictionary.structures.Tenant;
import org.entcore.feeder.export.Exporter;
import org.entcore.feeder.utils.Function;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

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
		String n = (String) vertx.sharedData().getLocalMap("server").get("node");
		this.node = (n != null) ? n : "";
		this.concatFiles = concatFiles;
		this.deleteExport = deleteExport;
	}

	@Override
	public void export(final Handler<Message<JsonObject>> handler) throws Exception {
		TransactionManager.executeTransaction(new Function<TransactionHelper, Message<JsonObject>>() {
			@Override
			public void apply(TransactionHelper value) {
				Tenant.list(new fr.wseduc.webutils.collections.JsonArray().add("name").add("academy"), null, null, value);
				Structure.list(ELIOT, new fr.wseduc.webutils.collections.JsonArray().add("academy"), null, null, value);
			}

			@Override
			public void handle(Message<JsonObject> result) {
				JsonArray r = result.body().getJsonArray("results");
				if ("ok".equals(result.body().getString("status")) && r != null && r.size() == 2) {
					final String tenant = r.getJsonArray(0).getJsonObject(0).getString("name");
					final String academy = r.getJsonArray(0).getJsonObject(0).getString("academy", r.getJsonArray(1).getJsonObject(0).getString("academy"));
					final Date exportDate = new Date();
					final String path = exportBasePath + File.separator +
							tenant + "_Complet_" + datetime.format(exportDate) + "_Export";
					log.info("Export path " + path);
					vertx.fileSystem().mkdirs(path, new Handler<AsyncResult<Void>>() {
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
														message.body().put("exportPath", path);
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
		vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> asyncResult) {
				if (asyncResult.succeeded()) {
					JsonObject j = new JsonObject()
							.put("path", new fr.wseduc.webutils.collections.JsonArray(asyncResult.result()))
							.put("zipFile", zipPath)
							.put("deletePath", true);
					vertx.eventBus().send(node + "entcore.zipper", j, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								sendWithWebDav(zipPath, handler);
								vertx.fileSystem().deleteRecursive(path, true, null);
							} else {
								log.error("Error zipping export : ");
								log.error(event.body().encode());
								handler.handle(event);
							}
						}
					}));
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
				.put("action", "put")
				.put("uri", exportDestination +
						file.substring(file.lastIndexOf(File.separator) + 1))
				.put("file", file);
		eb.send(node + WEBDAV_ADDRESS, j, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
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
		}));
	}

}
