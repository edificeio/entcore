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

import java.io.File;
import java.util.HashMap;
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
							JsonArray results = ja.getJsonObject(0).getJsonArray("results");
							queries.remove(tableName);
							if (results != null && !results.isEmpty()) {
								vertx.fileSystem().writeFile(filePath, results.toBuffer(),
										new Handler<AsyncResult<Void>>() {
											@Override
											public void handle(AsyncResult<Void> voidAsyncResult) {
												exportTables(queries, cumulativeResult.addAll(results), exportPath,
														exported, handler);
											}
										});
							} else {
								exportTables(queries, cumulativeResult, exportPath, exported, handler);
							}
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

}