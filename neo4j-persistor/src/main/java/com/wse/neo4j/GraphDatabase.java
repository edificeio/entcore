package com.wse.neo4j;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface GraphDatabase {

	void execute(String query, JsonObject params, Handler<JsonObject> handler);

	void executeBatch(JsonArray queries, Handler<JsonObject> handler);

	void batchInsert(String query, Handler<JsonObject> handler);

	void executeMultiple(Message<JsonObject> message);

	void close();

}
