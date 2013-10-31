package edu.one.core.common.neo4j;

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class Neo  {
	private JsonObject result = new JsonObject();
	private EventBus eb;
	private String address;
	private Logger log;

	public Neo (EventBus eb, Logger log) {
		this.eb = eb;
		this. log = log;
		this.address = "wse.neo4j.persistor";
	}

	public void sendBatch(JsonArray queries, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeBatch");
		jo.putArray("queries", queries);
		eb.send(address, jo, handler);
	}

	public void sendBatch(JsonArray queries, final HttpServerResponse response) {
		sendBatch(queries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	public void send(String query, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		eb.send(address, jo, handler);
	}

	public void send(String query) {
		send(query, (Handler<Message<JsonObject>>) null);
	}

	public void sendMultiple(String query, String rs, String as, String vs) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "executeMultiple");
		jo.putString("queries", query);
		jo.putString("requestSeparator", rs);
		jo.putString("attrSeparator", as);
		jo.putString("valueSeparator", vs);
		eb.send(address, jo);
	}

	public void send(String query, Map<String,Object> params, final HttpServerResponse response) {
		send(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	public void send(String query, Map<String,Object> params) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", new JsonObject(params));
		eb.send(address, jo);
	}

	public void send(String query, Map<String,Object> params, Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", new JsonObject(params));
		eb.send(address, jo , handler);
	}

	public void send(String query, final HttpServerResponse response) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		eb.send(address, jo , new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	// TODO : refactor Neo API
	public void send(final HttpServerRequest request) {
		JsonObject jo = new JsonObject();
		jo.putString("action", request.params().get("action"));
		jo.putString("query", request.params().get("query"));
		eb.send(address, jo , new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				request.response().putHeader("content-type", "text/json");
				request.response().end(m.body().encode());
			}
		});
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

}