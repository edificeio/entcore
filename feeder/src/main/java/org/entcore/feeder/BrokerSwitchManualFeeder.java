package org.entcore.feeder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.migration.AppMigrationConfiguration;
import org.entcore.common.migration.BrokerSwitchConfiguration;

public class BrokerSwitchManualFeeder implements IManualFeeder {

	private final IManualFeeder delegate;
	private final EventBus eventBus;
	private final AppMigrationConfiguration appMigrationConfiguration;

	public BrokerSwitchManualFeeder(IManualFeeder manualFeeder, EventBus eventBus, AppMigrationConfiguration appMigrationConfiguration) {
		this.delegate = manualFeeder;
		this.eventBus = eventBus;
		this.appMigrationConfiguration = appMigrationConfiguration;
	}

	private <T> Future<Message<T>> sendToBroker(String action, JsonObject params) {
		JsonObject body = new JsonObject()
				.put("action", action)
				.put("service", "referential")
				.put("params", params);
		final String address = BrokerSwitchConfiguration.LEGACY_MIGRATION_ADDRESS;
		return eventBus.request(address, body);
	}
	
	private <T> void handleBrokerResponse(AsyncResult<Message<T>> response, Message<JsonObject> message, String action) {
		if (response.succeeded()) {
			Message<T> result = response.result();
			if (result.body() instanceof JsonObject) {
				message.reply(result.body());
			} else {
				message.fail(500, "Invalid response from broker for action " + action);
			}
		} else {
			message.fail(500, "Error for action " + action + ": " + response.cause().getMessage());
		}
	}

	@Override
	public void createStructure(Message<JsonObject> message) {
		if (appMigrationConfiguration.isWriteNew()) {
			JsonObject structureInfos = message.body().getJsonObject("data");
			String action = "createStructure";
			sendToBroker(action, structureInfos)
					.onComplete(response -> handleBrokerResponse(response, message, action));
		}
		if (appMigrationConfiguration.isWriteLegacy()) {
			delegate.createStructure(message);
		}
	}

	@Override
	public void createClass(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateClass(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeClass(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void createUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUsers(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeUsers(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateUserLogin(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void deleteUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void restoreUser(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void createFunction(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void deleteFunction(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void deleteFunctionGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUserFunction(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUserHeadTeacherManual(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateUserHeadTeacherManual(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void createManualSubject(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateManualSubject(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void deleteManualSubject(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUserDirectionManual(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeUserDirectionManual(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeUserFunction(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addUserGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeUserGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void createOrUpdateTenant(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void createGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void deleteGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void addGroupUsers(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void removeGroupUsers(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void updateEmailGroup(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void structureAttachment(Message<JsonObject> message) {
		if (appMigrationConfiguration.isWriteNew()) {
			JsonObject structureInfos = new JsonObject()
					.put("structureId", message.body().getString("structureId"))
					.put("parentStructureId", message.body().getString("parentStructureId"));
			String action = "attachStructure";
			sendToBroker(action, structureInfos)
					.onComplete(response -> handleBrokerResponse(response, message, action));
		}
		if (appMigrationConfiguration.isWriteLegacy()) {
			delegate.createStructure(message);
		}
	}

	@Override
	public void structureDetachment(Message<JsonObject> message) {
		if (appMigrationConfiguration.isWriteNew()) {
			JsonObject structureInfos = new JsonObject()
					.put("structureId", message.body().getString("structureId"))
					.put("parentStructureId", message.body().getString("parentStructureId"));
			String action = "detachStructure";
			sendToBroker(action, structureInfos)
					.onComplete(response -> handleBrokerResponse(response, message, action));
		}
		if (appMigrationConfiguration.isWriteLegacy()) {
			delegate.createStructure(message);
		}
	}

	@Override
	public void updateStructure(Message<JsonObject> message) {
		if (appMigrationConfiguration.isWriteNew()) {
			JsonObject structureInfos = message.body().getJsonObject("data");
			String action = "updateStructure";
			sendToBroker(action, structureInfos)
					.onComplete(response -> handleBrokerResponse(response, message, action));
		}
		if (appMigrationConfiguration.isWriteLegacy()) {
			delegate.createStructure(message);
		}
	}

	@Override
	public void relativeStudent(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}

	@Override
	public void unlinkRelativeStudent(Message<JsonObject> message) {
		message.fail(500, "Not implemented yet");
	}
}
