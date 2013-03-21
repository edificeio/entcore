package edu.one.core.infra;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerResponse;
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

	public void send(String query) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		eb.send(address, jo);
	}

	public void send(String query, final HttpServerResponse response) {
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		eb.send(address, jo , new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				response.end(m.body.encode());
			}
		});
	}


}
