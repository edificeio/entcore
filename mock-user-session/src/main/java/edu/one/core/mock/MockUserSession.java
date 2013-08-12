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
		if (!"1234".equals(sessionId) && !"2345".equals(sessionId) && !"3456".equals(sessionId)) {
			sendError(message, "Invalid sessionId.");
			return;
		}

		JsonObject session = new JsonObject();
		if ("1234".equals(sessionId)) {
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
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|addDocument\", \"displayName\" : \"workspace.document.add\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|addRackDocument\", \"displayName\" : \"workspace.rack.document.add\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|copyRackDocument\", \"displayName\" : \"workspace.document.copy\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|copyRackDocuments\", \"displayName\" : \"workspace.rack.documents.copy\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|deleteRackDocument\", \"displayName\" : \"workspace.rack.document.delete\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|getRackDocument\", \"displayName\" : \"workspace.rack.document.get\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocuments\", \"displayName\" : \"workspace.documents.list\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocumentsByFolder\", \"displayName\" : \"workspace.documents.list.by.folder\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listFolders\", \"displayName\" : \"workspace.document.list.folders\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackDocuments\", \"displayName\" : \"workspace.rack.list.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackTrashDocuments\", \"displayName\" : \"workspace.rack.list.trash.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|moveTrashRack\", \"displayName\" : \"workspace.rack.document.move.trash\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|share\", \"displayName\" : \"workspace.share\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|shareDocument\", \"displayName\" : \"workspace.share.document\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|view\", \"displayName\" : \"workspace.view\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.registry.service.AppRegistryService|listApplicationsWithActions\", \"displayName\" : "
					+ "\"app-registry.list.applications.actions\", \"type\" : \"WORKFLOW\"}"
					+ "]";
			session.putString("userId", "4420000042");
			session.putString("firstName", "Nicolas");
			session.putString("lastName", "LOPEZ");
			session.putString("username", "Nicolas LOPEZ");
			session.putString("type", "SUPERADMIN");
			session.putString("classId", "4400000002$ORDINAIRE$CM2 de Mme Rousseau");
			session.putArray("authorizedActions", new JsonArray(actions));
		} else if ("2345".equals(sessionId)){
			String actions = "["
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|getRackDocument\", \"displayName\" : \"workspace.rack.document.get\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocuments\", \"displayName\" : \"workspace.documents.list\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocumentsByFolder\", \"displayName\" : \"workspace.documents.list.by.folder\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listFolders\", \"displayName\" : \"workspace.document.list.folders\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackDocuments\", \"displayName\" : \"workspace.rack.list.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackTrashDocuments\", \"displayName\" : \"workspace.rack.list.trash.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|addDocument\", \"displayName\" : \"workspace.document.add\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|share\", \"displayName\" : \"workspace.share\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|shareDocument\", \"displayName\" : \"workspace.share.document\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|view\", \"displayName\" : \"workspace.view\", \"type\" : \"WORKFLOW\"}"
					+ "]";
			session.putString("userId", "d6647393-cf6a-42da-978d-cd7e184aa0e7");
			session.putString("firstName", "Audrey");
			session.putString("lastName", "DULOUD");
			session.putString("username", "Audrey DULOUD");
			session.putString("type", "ELEVE");
			session.putString("classId", "74b97473-cd89-434d-801e-db689429bd65");
			session.putArray("authorizedActions", new JsonArray(actions));
		} else {
			String actions = "["
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|getRackDocument\", \"displayName\" : \"workspace.rack.document.get\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocuments\", \"displayName\" : \"workspace.documents.list\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listDocumentsByFolder\", \"displayName\" : \"workspace.documents.list.by.folder\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listFolders\", \"displayName\" : \"workspace.document.list.folders\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackDocuments\", \"displayName\" : \"workspace.rack.list.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|listRackTrashDocuments\", \"displayName\" : \"workspace.rack.list.trash.documents\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|addDocument\", \"displayName\" : \"workspace.document.add\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|share\", \"displayName\" : \"workspace.share\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|shareDocument\", \"displayName\" : \"workspace.share.document\", \"type\" : \"WORKFLOW\"}, "
					+ "{ \"name\" : \"edu.one.core.workspace.service.WorkspaceService|view\", \"displayName\" : \"workspace.view\", \"type\" : \"WORKFLOW\"}"
					+ "]";
			session.putString("userId", "14555fe0-af2b-4cf6-9a79-9e6bf0362cc9");
			session.putString("firstName", "Audrey");
			session.putString("lastName", "DULOUD");
			session.putString("username", "Audrey DULOUD");
			session.putString("type", "ELEVE");
			session.putString("classId", "9eb6bcf5-1bec-4f5f-88c3-87a4973944e4");
			session.putArray("authorizedActions", new JsonArray(actions));
		}
		sendOK(message, new JsonObject().putString("status", "ok").putObject("session", session));
	}

}
