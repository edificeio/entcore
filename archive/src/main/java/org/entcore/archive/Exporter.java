package org.entcore.archive;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;


public class Exporter extends BusModBase implements Handler<Message<JsonObject>> {

	@Override
	public void start() {
		super.start();
		vertx.eventBus().registerHandler(
				container.config().getString("address", "entcore.exporter"), this);
	}


	@Override
	public void handle(Message<JsonObject> event) {

	}

}
