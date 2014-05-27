/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.neo4j;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.Map;

public class Neo4j {

	private EventBus eb;
	private String address;
	private static Logger log = LoggerFactory.getLogger(Neo4j.class);

	private Neo4j() {}

	private static class Neo4jHolder {
		private static final Neo4j instance = new Neo4j();
	}

	public static Neo4j getInstance() {
		return Neo4jHolder.instance;
	}

	public void init(EventBus eb, String address) {
		this.eb = eb;
		this.address = address;
	}

	public void execute(String query, JsonObject params, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		if (params != null) {
			jo.putObject("params", params);
		}
		eb.send(address, jo, handler);
	}

	public void execute(String query, Map<String,Object> params, Handler<Message<JsonObject>> handler) {
		execute(query, new JsonObject(params), handler);
	}

	public void execute(String query, Map<String,Object> params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void execute(String query, JsonObject params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void executeBatch(JsonArray queries, final Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeBatch");
		jo.putArray("queries", queries);
		eb.send(address, jo, handler);
	}

	public void executeBatch(JsonArray queries, final HttpServerResponse response) {
		executeBatch(queries, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
			Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeTransaction");
		jo.putArray("statements", statements);
		jo.putBoolean("commit", commit);
		if (transactionId != null) {
			jo.putNumber("transactionId", transactionId);
		}
		eb.send(address, jo, handler);
	}

	public void resetTransactionTimeout(int transactionId, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "resetTransactionTimeout");
		jo.putNumber("transactionId", transactionId);
		eb.send(address, jo, handler);
	}

	public void rollbackTransaction(int transactionId, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "rollbackTransaction");
		jo.putNumber("transactionId", transactionId);
		eb.send(address, jo, handler);
	}

}