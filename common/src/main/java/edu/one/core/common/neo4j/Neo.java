package edu.one.core.common.neo4j;

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class Neo  {
	private EventBus eb;
	private String address;
	private Logger log;

	public Neo (EventBus eb, Logger log) {
		this.eb = eb;
		this. log = log;
		this.address = "wse.neo4j.persistor";
	}

	@Deprecated
	public void sendBatch(JsonArray queries, final Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeBatch");
		jo.putArray("queries", queries);
		eb.send(address, jo, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray results = event.body().getArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null) {
					for (Object o : results) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						int i = 0;
						JsonObject r = new JsonObject();
						for (Object o2 : j.getArray("result")) {
							if (!(o2 instanceof JsonObject)) continue;
							r.putObject(String.valueOf(i++), (JsonObject) o2);
						}
						j.putObject("result", r);
					}
				}
				handler.handle(event);
			}
		});
	}

	@Deprecated
	public void sendBatch(JsonArray queries, final HttpServerResponse response) {
		sendBatch(queries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void send(String query, Handler<Message<JsonObject>> handler) {
		send(query, null, handler);
	}

	public void send(String query) {
		send(query, (Map<String,Object>) null);
	}

	@Deprecated
	public void send(String query, Map<String,Object> params, final HttpServerResponse response) {
		send(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	public void send(String query, Map<String,Object> params) {
		send(query, params, (Handler<Message<JsonObject>>) null);
	}

	@Deprecated
	public void send(String query, Map<String,Object> params, final Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		if (params != null) {
			jo.putObject("params", new JsonObject(params));
		}
		if (handler != null) {
			eb.send(address, jo, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					log.debug(event.body().encode());
					JsonArray result = event.body().getArray("result");
					if ("ok".equals(event.body().getString("status")) && result != null) {
						int i = 0;
						JsonObject r = new JsonObject();
						for (Object o : result) {
							if (!(o instanceof JsonObject)) continue;
							r.putObject(String.valueOf(i++), (JsonObject) o);
						}
						event.body().putObject("result", r);
					}
					handler.handle(event);
				}
			});
		} else {
			eb.send(address, jo);
		}
	}

	@Deprecated
	public void send(String query, final HttpServerResponse response) {
		send(query, null, response);
	}

	public static JsonObject toJsonObject(String query, JsonObject params) {
		return new JsonObject()
		.putString("query", query)
		.putObject("params", (params != null) ? params : new JsonObject());
	}

	public static JsonArray resultToJsonArray(JsonObject j) {
		JsonArray r = new JsonArray();
		if (j != null) {
			for (String idx : j.getFieldNames()) {
				r.addObject(j.getObject(idx));
			}
		}
		return r;
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