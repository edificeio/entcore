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

package org.entcore.common.sql;

import fr.wseduc.webutils.Utils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DB {

	private static final Logger log = LoggerFactory.getLogger(DB.class);

	public static void loadScripts(final String schema, final Vertx vertx, final String path) {
		final String s = (schema != null && !schema.trim().isEmpty()) ? schema + "." : "";
		final Sql sql = Sql.getInstance();
		String query = "SELECT count(*) FROM information_schema.tables WHERE table_name = 'scripts'" +
			" AND table_schema = '" + ((!s.isEmpty()) ? schema : "public") + "'";
		sql.raw(query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Long nb = SqlResult.countResult(message);
				if (nb == null) {
					log.error("Error loading sql scripts.");
				} else if (nb == 1) {
					sql.raw("SELECT filename FROM " + s + "scripts", new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> message) {
							if ("ok".equals(message.body().getString("status"))) {
								JsonArray fileNames = Utils.flatten(message.body().getJsonArray("results"));
								loadAndExecute(s, vertx, path, fileNames);
							}
						}
					});
				} else if (nb == 0) {
					loadAndExecute(s, vertx, path, new JsonArray());
				}
			}
		});
	}

	private static void loadAndExecute(final String schema, final Vertx vertx,
			final String path, final JsonArray excludeFileNames) {
		vertx.fileSystem().readDir(path, ".*?\\.sql$", new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> asyncResult) {
				if (asyncResult.succeeded()) {
					final List<String> files = asyncResult.result();
					Collections.sort(files);
					final SqlStatementsBuilder s = new SqlStatementsBuilder();
					final JsonArray newFiles = new JsonArray();
					final AtomicInteger count = new AtomicInteger(files.size());
					for (final String f : files) {
						final String filename = f.substring(f.lastIndexOf(File.separatorChar) + 1);
						if (!excludeFileNames.contains(filename)) {
							vertx.fileSystem().readFile(f, new Handler<AsyncResult<Buffer>>() {
								@Override
								public void handle(AsyncResult<Buffer> bufferAsyncResult) {
									if (bufferAsyncResult.succeeded()) {
										String script = bufferAsyncResult.result().toString();
										script = script.replaceAll("\\-\\-\\s.*(\r|\n|$)", "").replaceAll("(\r|\n|\t)", " ");
										s.raw(script);
										newFiles.add(new JsonArray().add(filename));
									} else {
										log.error("Error reading file : " + f, bufferAsyncResult.cause());
									}
									if (count.decrementAndGet() == 0) {
										commit(schema, s, newFiles);
									}
								}
							});
						} else {
							count.decrementAndGet();
						}
					}
					if (count.get() == 0 && newFiles.size() > 0) {
						commit(schema, s, newFiles);
					}
				} else {
					log.error("Error reading sql directory : " + path, asyncResult.cause());
				}
			}

			private void commit(final String schema, SqlStatementsBuilder s, final JsonArray newFiles) {
				s.insert(schema + "scripts", new JsonArray().add("filename"), newFiles);
				Sql.getInstance().transaction(s.build(), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						if ("ok".equals(message.body().getString("status"))) {
							log.info("Scripts added : " + newFiles.encode());
						} else {
							log.error("Error when commit transaction : " + message.body().getString("message"));
						}
					}
				});
			}
		});
	}

}
