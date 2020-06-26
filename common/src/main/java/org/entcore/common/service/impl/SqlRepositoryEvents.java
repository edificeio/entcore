package org.entcore.common.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SqlRepositoryEvents extends AbstractRepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(SqlRepositoryEvents.class);
	protected final Sql sql = Sql.getInstance();
    protected final FolderImporter fileImporter;

    protected String mainResourceName = null;

	protected SqlRepositoryEvents(Vertx vertx) {
		super(vertx);
		fileImporter = new FolderImporter(vertx.fileSystem(), vertx.eventBus());
	}

    protected void exportTables(HashMap<String, JsonArray> queries, JsonArray cumulativeResult, HashMap<String, JsonArray> fieldsToNull,
            String exportPath, AtomicBoolean exported, Handler<Boolean> handler) {
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
                        if (ja != null && !ja.isEmpty())
                        {
                            JsonObject results = new JsonObject();
                            JsonArray fields = ja.getJsonObject(0).getJsonArray("fields");
                            JsonArray rows = ja.getJsonObject(0).getJsonArray("results");

                            JsonArray tableFieldsToNull = fieldsToNull == null ? null : fieldsToNull.get(tableName);

                            if(tableFieldsToNull != null)
                            {
                                for(int i = tableFieldsToNull.size(); i-- > 0;)
                                {
                                    String field = tableFieldsToNull.getString(i);
                                    int fieldIx = -1;
                                    for(int f = fields.size(); f-- > 0;)
                                    {
                                        if(fields.getString(f).equals(field))
                                        {
                                            fieldIx = f;
                                            break;
                                        }
                                    }

                                    if(fieldIx != -1)
                                    {
                                        for(int r = rows.size(); r-- > 0;)
                                        {
                                            JsonArray row = rows.getJsonArray(r);
                                            fr.wseduc.webutils.collections.JsonArray.setInJsonArray(row, fieldIx, null);
                                        }
                                    }
                                }
                            }

							results.put("fields", fields);
							results.put("results", rows);
                            queries.remove(tableName);

							vertx.fileSystem().writeFile(filePath, results.toBuffer(),
									new Handler<AsyncResult<Void>>() {
										@Override
										public void handle(AsyncResult<Void> voidAsyncResult) {
											exportTables(queries, cumulativeResult.add(results), fieldsToNull, exportPath,
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
                                String userId, String username, String locale, SqlStatementsBuilder builder,
                                boolean forceImportAsDuplication, Handler<JsonObject> handler)
    {
        getDuplicateSuffix(locale).setHandler(new Handler<AsyncResult<String>>()
        {
            @Override
            public void handle(AsyncResult<String> suffix)
            {
                importTables(importPath, schema, tables, tablesWithId, userId, username, locale, builder,
                    forceImportAsDuplication, handler, new HashMap<String, JsonObject>(), suffix.result());
            }
        });
    }

    protected void importTables(String importPath, String schema, List<String> tables, Map<String,String> tablesWithId,
                                String userId, String username, String locale, SqlStatementsBuilder builder, boolean forceImportAsDuplication,
                                Handler<JsonObject> handler, Map<String, JsonObject> idsMapByTable, String duplicateSuffix)
    {
        if (tables.isEmpty()) {
            tablesWithId.keySet().forEach(table -> {
                if (tablesWithId.get(table).equals("DEFAULT")) {
                    builder.raw("UPDATE " + schema + "." + table + " AS mytable " +
                            "SET id = DEFAULT WHERE " +
                            "(SELECT (CASE WHEN mytable.id > (SELECT (CASE WHEN is_called THEN last_value ELSE 0 END)" +
                            " FROM " + schema + "." + table + "_id_seq) THEN TRUE " +
                            "ELSE FALSE END))");
                }
            });
            JsonArray statements = builder.build();
            importDocumentsDependancies(importPath, userId, username, statements, done -> {
                sql.transaction(done, message -> {
                    int resourcesNumber = 0, duplicatesNumber = 0, errorsNumber = 0;
                    if ("ok".equals(message.body().getString("status")))
                    {
                        JsonArray results = message.body().getJsonArray("results");
                        Map<String, Integer> dupsMap = new HashMap<String, Integer>();

                        for (int i = 0; i < results.size(); i++)
                        {
                            JsonObject jo = results.getJsonObject(i);

                            if (!"ok".equals(jo.getString("status"))) {
                                errorsNumber++;
                            } else {
                                if (jo.getJsonArray("fields").contains("duplicates"))
                                {
                                    String collec = jo.getJsonArray("results").getJsonArray(0).getString(0);
                                    Integer dups = jo.getJsonArray("results").getJsonArray(0).getInteger(1);
                                    Integer oldDups = dupsMap.getOrDefault(collec, 0);

                                    dupsMap.put(collec, oldDups + dups);
                                    duplicatesNumber += dups;
                                }
                                if (jo.getJsonArray("fields").contains("noduplicates")) {
                                    resourcesNumber += jo.getJsonArray("results").getJsonArray(0).getInteger(1);
                                }
                            }
                        }

                        JsonObject finalMap = new JsonObject();
                        for(Map.Entry<String, JsonObject> entry : idsMapByTable.entrySet())
                            finalMap.put(entry.getKey(), entry.getValue());

                        JsonObject reply =
                            new JsonObject()
                                .put("status","ok")
                                .put("resourcesNumber", String.valueOf(resourcesNumber))
                                .put("errorsNumber", String.valueOf(errorsNumber))
                                .put("duplicatesNumber", String.valueOf(duplicatesNumber))
                                .put("resourcesIdsMap", finalMap)
                                .put("duplicatesNumberMap", dupsMap)
                                .put("mainResourceName", mainResourceName);

                        log.info(title + " : Imported "+ resourcesNumber + " resources (" + duplicatesNumber + " duplicates) with " + errorsNumber + " errors." );
                        handler.handle(reply);
                    } else {
                        log.error(title + " Import error: " + message.body().getString("message"));
                        handler.handle(new JsonObject().put("status", "error"));
                    }

                });
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

                    int idIx = 0;
                    for(int i = 0; i < fields.size(); ++i)
                    {
                        if(fields.getString(i).equals("id"))
                        {
                            idIx = i;
                            break;
                        }
                    }

                    Map<String, Object> oldIdsToNewIdsMap = new HashMap<String, Object>();
                    for(int i = results.size(); i-- > 0;)
                    {
                        String id = "";
                        try
                        {
                            id = results.getJsonArray(i).getString(idIx);
                        }
                        catch(ClassCastException e)
                        {
                            id = results.getJsonArray(i).getInteger(idIx).toString();
                        }
                        oldIdsToNewIdsMap.put(id, id);
                    }

                    idsMapByTable.put(table, new JsonObject(oldIdsToNewIdsMap));

                    if (!results.isEmpty()) {

                        results = transformResults(fields, results, userId, username, builder, table, forceImportAsDuplication, duplicateSuffix);

                        String insert = "WITH rows AS (INSERT INTO " + schema + "." + table + " (" + String.join(",",
                                ((List<String>) fields.getList()).stream().map(f -> "\"" + f + "\"").toArray(String[]::new)) + ") VALUES ";
                        String conflictUpdate = "ON CONFLICT(id) DO UPDATE SET id = ";
                        String conflictNothing = "ON CONFLICT DO NOTHING RETURNING 1) SELECT '" + table + "' AS table, "
                                                + "count(*) AS " + (tablesWithId.containsKey(table) ? "duplicates" : "noduplicates") + " FROM rows";

                        for (int i = 0; i < results.size(); i++) {
                            JsonArray entry = results.getJsonArray(i);
                            String query = insert + Sql.listPrepared(entry);

                            if (tablesWithId.containsKey(table)) {
                                builder.prepared(query + conflictUpdate + tablesWithId.get(table) +
                                        " RETURNING 1) SELECT '" + table + "' AS table, count(*) AS noduplicates FROM rows", entry);
                            }
                            builder.prepared(query + conflictNothing, entry);
                        }

                    }
                    importTables(importPath, schema, tables, tablesWithId, userId, username, locale, builder,
                        forceImportAsDuplication, handler, idsMapByTable, duplicateSuffix);
                }
            });
        }
    }

	protected JsonArray transformResults(JsonArray fields, JsonArray results, String userId, String username,
                                         SqlStatementsBuilder builder, String table, boolean forceImportAsDuplication,
                                         String duplicationSuffix){ return results; }

    protected void importDocumentsDependancies(String importPath, String userId, String userName, JsonArray statements,
                                             Handler<JsonArray> handler) {
	    final String filePath = importPath + File.separator + "Documents";
        fs.exists(filePath, exist -> {
            if (exist.succeeded() && exist.result().booleanValue()) {
                FolderImporter.FolderImporterContext ctx = new FolderImporter.FolderImporterContext(filePath, userId, userName);
                fileImporter.importFoldersFlatFormat(ctx, rapport -> {
                    fileImporter.applyFileIdsChange(ctx, statements.getList());
                    handler.handle(statements);
                });
            } else {
                handler.handle(statements);
            }
        });
    }
}