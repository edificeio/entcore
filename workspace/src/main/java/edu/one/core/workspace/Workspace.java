package edu.one.core.workspace;

import edu.one.core.infra.MongoDb;
import edu.one.core.infra.Server;
import edu.one.core.common.http.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.workspace.security.WorkspaceResourcesProvider;
import edu.one.core.workspace.service.WorkspaceService;
import org.vertx.java.core.Future;

public class Workspace extends Server {

	@Override
	public void start(final Future<Void> result) {
		super.start();

		final MongoDb mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));

		WorkspaceService service = new WorkspaceService(vertx, container, rm, trace, securedActions);

		service.get("/workspace", "view");

		service.get("/share/json/:id", "shareJson");

		service.put("/share/json/:id", "shareJsonSubmit");

		service.put("/share/remove/:id", "removeShare");

		service.post("/document", "addDocument");

		service.get("/document/:id", "getDocument");

		service.put("document/:id", "updateDocument");

		service.delete("/document/:id", "deleteDocument");

		service.get("/documents", "listDocuments");

		service.get("/documents/:folder", "listDocumentsByFolder");

		service.put("/document/move/:id/:folder", "moveDocument");

		service.put("/documents/move/:ids", "moveDocuments");

		service.put("/documents/move/:ids/:folder", "moveDocuments");

		service.put("/document/trash/:id", "moveTrash");

		service.put("/restore/document/:id", "restoreTrash");

		service.post("/documents/copy/:ids", "copyDocuments");

		service.post("/documents/copy/:ids/:folder", "copyDocuments");

		service.post("/document/copy/:id/:folder", "copyDocument");

		service.post("/document/:id/comment", "commentDocument");

		service.get("/folders", "listFolders");

		service.get("/folders/list", "folders");

		service.post("/folder", "addFolder");

		service.put("/folder/move/:id", "moveFolder");

		service.put("/folder/copy/:id", "copyFolder");

		service.put("/folder/trash/:id", "moveTrashFolder");

		service.put("/folder/restore/:id", "restoreFolder");

		service.delete("/folder/:id", "deleteFolder");

		service.post("/rack/:to", "addRackDocument");

		service.get("/rack/documents", "listRackDocuments");

		service.get("/rack/:id", "getRackDocument");

		service.delete("/rack/:id", "deleteRackDocument");

		service.post("/rack/documents/copy/:ids", "copyRackDocuments");

		service.post("/rack/documents/copy/:ids/:folder", "copyRackDocuments");

		service.post("/rack/document/copy/:id/:folder", "copyRackDocument");

		service.put("/rack/trash/:id", "moveTrashRack");

		service.put("/restore/rack/:id", "restoreTrashRack");

		service.get("/rack/documents/Trash", "listRackTrashDocuments");

		service.get("/users/available-rack", "rackAvailableUsers");

		service.get("/workspace/availables-workflow-actions", "getActionsInfos");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(),
				Server.getEventBus(vertx), new WorkspaceResourcesProvider(mongo)));

	}

}
