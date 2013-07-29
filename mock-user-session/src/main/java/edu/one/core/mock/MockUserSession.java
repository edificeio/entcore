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

		String actions = "[{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listApplications\", \"displayName\" : "
				+ "\"app-registry.list.applications\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listApplicationActions\", \"displayName\" : "
				+ "\"app-registry.list.actions\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|createRole\", \"displayName\" : "
				+ "\"app-registry.create.role\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listRoles\", \"displayName\" : "
				+ "\"app-registry.list.roles\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listRolesWithActions\", \"displayName\" : "
				+ "\"app-registry.list.roles.actions\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|view\", \"displayName\" : "
				+ "\"app-registry.view\", \"type\" : \"WORKFLOW\"}, "
				+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listApplicationsWithActions\", \"displayName\" : "
				+ "\"app-registry.list.applications.actions\", \"type\" : \"WORKFLOW\"}"
				+ "]";
		JsonObject session = new JsonObject();
		session.putString("userId", "4420000042");
		session.putString("firstName", "Nicolas");
		session.putString("lastName", "LOPEZ");
		session.putString("username", "Nicolas LOPEZ");
		session.putString("classId", "4400000002$ORDINAIRE$CM2 de Mme Rousseau");
		session.putArray("authorizedActions", new JsonArray(actions));
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

}
