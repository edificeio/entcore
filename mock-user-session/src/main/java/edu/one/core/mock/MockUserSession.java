package edu.one.core.mock;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class MockUserSession extends BusModBase implements Handler<Message<JsonObject>> {

	protected String address;

	public void start() {
		super.start();
		address = getOptionalStringConfig("address", "wse.mock.session");
		eb.registerHandler(address, this);
	}

	@Override
	public void handle(Message<JsonObject> message) {
		String action = message.body().getString("action");

		if (action == null) {
			sendError(message, "action must be specified");
			return;
		}

		switch (action) {
		case "find":
			doFind(message);
			break;
		default:
			sendError(message, "Invalid action: " + action);
		}
	}

	private void doFind(Message<JsonObject> message) {
		String sessionId = message.body().getString("sessionId");
		if (!"1234".equals(sessionId)) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session = new JsonObject();
		session.putString("userId", "42d93f59-9b12-417d-b998-45b18bdd5afa");
		session.putString("username", "blip");
		session.putArray("authorizedActions", new JsonArray());
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

}
