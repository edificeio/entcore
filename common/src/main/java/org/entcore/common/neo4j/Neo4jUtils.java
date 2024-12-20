/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.neo4j;

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

public class Neo4jUtils {

	private static final Logger log = LoggerFactory.getLogger(Neo4jUtils.class);
	private static final String UPDATE_SCRIPTS =
			"MERGE (n:System {name : {appName}}) SET n.scripts = coalesce(n.scripts, []) + {newFiles} ";

	public static String nodeSetPropertiesFromJson(String nodeAlias, JsonObject json, String... ignore) {
		StringBuilder sb = new StringBuilder();
		List<String> i;
		if (ignore != null) {
			i = Arrays.asList(ignore);
		} else {
			i = Collections.emptyList();
		}
		for (String a: json.fieldNames()) {
			String attr = a.replaceAll("\\W+", "");
			if (i.contains(attr)) continue;
			sb.append(", ").append(nodeAlias).append(".").append(attr).append(" = {").append(attr).append("}");
		}
		if (sb.length() > 2) {
			return sb.append(" ").substring(2);
		}
		return " ";
	}

	public static void loadScripts(final String appName, final Vertx vertx, final String path) {
		String query = "MATCH (n:System) WHERE n.name = {appName} RETURN n.scripts as scripts";
		Neo4j.getInstance().execute(query, new JsonObject().put("appName", appName), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("result");
				JsonArray scripts;
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() > 0) {
					scripts = res.getJsonObject(0).getJsonArray("scripts", new fr.wseduc.webutils.collections.JsonArray());
				} else {
					scripts = new fr.wseduc.webutils.collections.JsonArray();
				}
				loadAndExecute(appName, vertx, path, true, scripts);
				loadAndExecute(appName, vertx, path, false, scripts);
			}
		});
	}

	private static void loadAndExecute(final String schema, final Vertx vertx, final String path,
			final boolean index, final JsonArray excludeFileNames) {
		final String pattern = index ? ".*?-index\\.cypher$" : "^(?!.*-index).*?\\.cypher$";
		vertx.fileSystem().readDir(path, pattern, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> asyncResult) {
				if (asyncResult.succeeded()) {
					final List<String> files = asyncResult.result();
					Collections.sort(files);
					final StatementsBuilder s = new StatementsBuilder();
					final JsonArray newFiles = new fr.wseduc.webutils.collections.JsonArray();
					final AtomicInteger count = new AtomicInteger(files.size());
					for (final String f : files) {
						final String filename = f.substring(f.lastIndexOf(File.separatorChar) + 1);
						if (!excludeFileNames.contains(filename)) {
							vertx.fileSystem().readFile(f, new Handler<AsyncResult<Buffer>>() {
								@Override
								public void handle(AsyncResult<Buffer> bufferAsyncResult) {
									if (bufferAsyncResult.succeeded()) {
										String script = bufferAsyncResult.result().toString();
										for (String q : script.replaceAll("(\r|\n)", " ").split(";")) {
											s.add(q);
										}
										newFiles.add(filename);
									} else {
										log.error("Error reading file : " + f, bufferAsyncResult.cause());
									}
									if (count.decrementAndGet() == 0) {
										commit(schema, s, newFiles, index);
									}
								}
							});
						} else if (count.decrementAndGet() == 0 && newFiles.size() > 0) {
							commit(schema, s, newFiles, index);
						}
					}
				} else if (log.isDebugEnabled()) {
					log.debug("Error reading neo4j directory : " + path, asyncResult.cause());
				}
			}

			private void commit(final String schema, StatementsBuilder s, final JsonArray newFiles, final boolean index) {
				final JsonObject params = new JsonObject().put("appName", schema).put("newFiles", newFiles);
				if (!index) {
					s.add(UPDATE_SCRIPTS, params);
				}
				Neo4j.getInstance().executeTransaction(s.build(), null, true, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						if ("ok".equals(message.body().getString("status"))) {
							if (index) {
								Neo4j.getInstance().execute(UPDATE_SCRIPTS, params, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (!"ok".equals(event.body().getString("status"))) {
											log.error("Error update scripts : " + event.body().getString("message"));
										}
									}
								});
							}
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
