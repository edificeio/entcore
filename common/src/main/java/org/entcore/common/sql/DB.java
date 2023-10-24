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

package org.entcore.common.sql;

import fr.wseduc.webutils.Utils;
import io.vertx.core.*;
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

	private Sql sql;
	private String schema;
	private Vertx vertx;

	public DB( final Vertx vertx, final Sql sql, final String schema ) {
		this.vertx = vertx;
		this.sql = sql;
		this.schema = schema;
	}

	public Future<Void> loadScripts(final String path) {
		final Promise<Void> promise = Promise.promise();
		if( sql == null ) {
			log.warn("Sql instance is null.");
			promise.complete();
		} else {
			final String s = (schema != null && !schema.trim().isEmpty()) ? schema + "." : "";
			String query = "SELECT count(*) FROM information_schema.tables WHERE table_name = 'scripts'" +
				" AND table_schema = '" + ((!s.isEmpty()) ? schema : "public") + "'";
			sql.raw(query, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					Long nb = SqlResult.countResult(message);
					if (nb == null) {
						log.error("Error loading sql scripts while executing " + query + ". Body = " + message.body().encode());
						promise.tryFail("Error loading sql scripts.");
					} else if (nb == 1) {
						sql.raw("SELECT filename FROM " + s + "scripts", new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> message) {
								if ("ok".equals(message.body().getString("status"))) {
									JsonArray fileNames = Utils.flatten(message.body().getJsonArray("results"));
									loadAndExecute(s, path, fileNames).onComplete(promise);
								} else {
									promise.tryFail("Error while fetching already played scripts : " + message.body().getString("message"));
								}
							}
						});
					} else if (nb == 0) {
						loadAndExecute(s, path, new JsonArray()).onComplete(promise);
					}
				}
			});
		}
		return promise.future();
	}

	private Future<Void> loadAndExecute(final String schema,
			final String path, final JsonArray excludeFileNames) {
		final Promise<Void> promise = Promise.promise();
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
							vertx.fileSystem().readFile(f, bufferAsyncResult -> {
                if (bufferAsyncResult.succeeded()) {
                  String script = bufferAsyncResult.result().toString();
                  script = script.replaceAll("\\-\\-\\s.*(\r|\n|$)", "").replaceAll("(\r|\n|\t)", " ");
                  s.raw(script);
                  newFiles.add(new JsonArray().add(filename));
                } else {
                  log.error("Error reading file : " + f, bufferAsyncResult.cause());
                }
                if (count.decrementAndGet() == 0) {
                  commit(schema, s, newFiles).onComplete(promise);
                }
              });
						} else {
							count.decrementAndGet();
						}
					}
					if (count.get() == 0 && newFiles.size() > 0) {
						commit(schema, s, newFiles).onComplete(promise);
					}
				} else {
					log.error("Error reading sql directory : " + path, asyncResult.cause());
					promise.tryFail(asyncResult.cause());
				}
			}

			private Future<Void> commit(final String schema, SqlStatementsBuilder s, final JsonArray newFiles) {
				final Promise<Void> promise = Promise.promise();
				s.insert(schema + "scripts", new JsonArray().add("filename"), newFiles);
				sql.transaction(s.build(), message -> {
          if ("ok".equals(message.body().getString("status"))) {
            log.info("Scripts added : " + newFiles.encode());
            promise.complete();
          } else {
            final String error = message.body().getString("message");
            log.error("Error when commit transaction : " + error);
            promise.fail(error);
          }
        });
				return promise.future();
			}
		});
		return promise.future();
	}

}
