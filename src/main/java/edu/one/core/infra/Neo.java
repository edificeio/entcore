/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.infra;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author rafik
 */
public class Neo implements Handler<Message<JsonObject>> {

	private JsonObject result = new JsonObject();
	private EventBus eb;
	private String address;

	public Neo (EventBus eb, String address) {
		this.eb = eb;
		this.address = address;
	}

	public JsonObject send(JsonObject jo) {
		eb.send(address, jo, this);
		
		return result ;
	}

	@Override
	public void handle(Message<JsonObject> m) {
		result = result.mergeIn(m.body);
	}

}
