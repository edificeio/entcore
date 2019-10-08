package org.entcore.common.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SqlRepositoryEvents extends AbstractRepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(SqlRepositoryEvents.class);
	protected final Sql sql = Sql.getInstance();

	protected SqlRepositoryEvents(Vertx vertx) {
		super(vertx);
	}

	protected void exportTables(HashMap<String, JsonArray> queries, JsonArray cumulativeResult, String exportPath,
			AtomicBoolean exported, Handler<Boolean> handler) {
		if (queries.isEmpty()) {
			exportDocumentsDependancies(cumulativeResult, exportPath, new Handler<Boolean>() {
				@Override
				public void handle(Boolean bool) {
					if (bool) {
						log.info(title + " exported successfully to : " + exportPath);
						exported.set(true);
						handler.handle(exported.get());
					} else {
						// Should never happen, export doesn't fail if docs export fail.
						handler.handle(exported.get());
					}
				}
			});
		} else {
			Map.Entry<String, JsonArray> entry = queries.entrySet().iterator().next();
			String tableName = entry.getKey();
			String filePath = exportPath + File.separator + tableName;
			JsonArray query = entry.getValue();
			sql.transaction(query, new Handler<Message<JsonObject>>() {
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray ja = event.body().getJsonArray("results");
						if (ja != null && !ja.isEmpty()) {
							JsonObject results = new JsonObject();
							results.put("fields",ja.getJsonObject(0).getJsonArray("fields"));
							results.put("results",ja.getJsonObject(0).getJsonArray("results"));
							queries.remove(tableName);
							vertx.fileSystem().writeFile(filePath, results.toBuffer(),
									new Handler<AsyncResult<Void>>() {
										@Override
										public void handle(AsyncResult<Void> voidAsyncResult) {
											exportTables(queries, cumulativeResult.add(results), exportPath,
													exported, handler);
										}
							});
						} else {
							log.error(title + " : Error, unexpected result " + event.body().encode());
							handler.handle(exported.get());
						}
					} else {
						log.error(title + " : Could not proceed query " + query);
						handler.handle(exported.get());
					}
				}
			});
		}
	}

    protected void importTables(String importPath, String schema, List<String> tables, Map<String,String> tablesWithId,
                                String userId, String username, SqlStatementsBuilder builder, Handler<JsonObject> handler) {
        if (tables.isEmpty()) {
            sql.transaction(builder.build(), message -> {
                int resourcesNumber = 0, duplicatesNumber = 0, errorsNumber = 0;
                if ("ok".equals(message.body().getString("status"))) {
                    JsonArray results = message.body().getJsonArray("results");
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject jo = results.getJsonObject(i);
                        if (!"ok".equals(jo.getString("status"))) {
                            errorsNumber++;
                        } else {
                            if (jo.getJsonArray("fields").contains("duplicates")) {
                                duplicatesNumber += jo.getJsonArray("results").getJsonArray(0).getInteger(0);
                            }
                            if (jo.getJsonArray("fields").contains("noduplicates")) {
                                resourcesNumber += jo.getJsonArray("results").getJsonArray(0).getInteger(0);
                            }
                        }
                    }
                    JsonObject reply = new JsonObject().put("status","ok").put("resourcesNumber",String.valueOf(resourcesNumber))
                            .put("errorsNumber",String.valueOf(errorsNumber)).put("duplicatesNumber", String.valueOf(duplicatesNumber));
                    log.info(title + " : Imported "+ resourcesNumber + " resources (" + duplicatesNumber + " duplicates) with " + errorsNumber + " errors." );
                    handler.handle(reply);
                } else {
                    log.error(title + " Import error: " + message.body().getString("message"));
                    handler.handle(new JsonObject().put("status", "error"));
                }

            });
        } else {
            String table = tables.remove(0);
            String path = importPath + File.separator + schema + "." + table;
            fs.readFile(path, result -> {
                if (result.failed()) {
                    log.error(title
                            + " : Failed to read table "+ schema + "." + table + " in archive.");
                    handler.handle(new JsonObject().put("status", "error"));
                } else {
                    JsonObject tableContent = result.result().toJsonObject();
                    JsonArray fields = tableContent.getJsonArray("fields");
                    JsonArray results = tableContent.getJsonArray("results");

                    if (!results.isEmpty()) {

                        results = transformResults(fields, results, userId, username, builder, table);

                        String fields_str = "(" + String.join(",",
                                ((List<String>) fields.getList()).stream().map(f -> "\"" + f + "\"").toArray(String[]::new)) + ")";
                        String query = "WITH rows AS (INSERT INTO " + schema + "." + table + " " +
                                fields_str + " VALUES ";
                        JsonArray params = new JsonArray();

                        for (int i = 0; i < results.size(); i++) {
                            JsonArray entry = results.getJsonArray(i);
                            query += Sql.listPrepared(entry);
                            query += (i == results.size() - 1) ? " " : ", ";
                            params.addAll(entry);
                        }

                        String conflictNothing = "ON CONFLICT DO NOTHING RETURNING 1 ) SELECT count(*) AS " + (tablesWithId.containsKey(table) ? "duplicates" : "noduplicates") + " FROM rows";

                        if (tablesWithId.containsKey(table)) {
                            String conflictUpdate = "ON CONFLICT(id) DO UPDATE SET id = "+tablesWithId.get(table)+" RETURNING 1 ) SELECT count(*) AS noduplicates FROM rows";
                            builder.prepared(query + conflictUpdate, params);
                        }
                        builder.prepared(query + conflictNothing, params);

                    }
                    importTables(importPath, schema, tables, tablesWithId, userId, username, builder, handler);
                }
            });
        }
    }

	protected JsonArray transformResults(JsonArray fields, JsonArray results, String userId, String username,
                                         SqlStatementsBuilder builder, String table){ return results; }

}