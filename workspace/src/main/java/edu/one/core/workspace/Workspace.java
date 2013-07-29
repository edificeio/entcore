package edu.one.core.workspace;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Controller;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.workspace.security.WorkspaceResourcesProvider;
import edu.one.core.workspace.service.WorkspaceService;

public class Workspace extends Controller {

	@Override
	public void start(final Future<Void> result) {
		super.start();

		// Mongodb config
		JsonObject mongodbConf = container.config().getObject("mongodb-config");
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-CR3-SNAPSHOT-WSE", mongodbConf, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					result.setResult(null);
				} else {
					log.error(ar.cause().getMessage());
				}
			}
		});
		final MongoDb mongo = new MongoDb(vertx.eventBus(), mongodbConf.getString("address"));

		JsonObject gridfsConf = container.config().getObject("gridfs-config");
		container.deployModule("com.wse~gridfs-persistor~0.1.0-SNAPSHOT", gridfsConf);

		WorkspaceService service = new WorkspaceService(vertx, container, rm, securedActions);

		service.get("/workspace", "view", this);

		service.get("/share", "share", this);

		service.post("/share", "shareDocument");

		service.post("/document", "addDocument");

		service.get("/document/:id", "getDocument");

		service.put("document/:id", "updateDocument");

		service.delete("/document/:id", "deleteDocument");

		service.get("/documents", "listDocuments");

		service.get("/documents/:folder", "listDocumentsByFolder");

		service.put("/document/move/:id/:folder", "moveDocument");

		service.put("/documents/move/:ids/:folder", "moveDocuments");

		service.put("/document/trash/:id", "moveTrash");

		service.post("/documents/copy/:ids/:folder", "copyDocuments");

		service.post("/document/copy/:id/:folder", "copyDocument");

		service.post("/document/:id/comment", "commentDocument");

		service.get("/folders", "listFolders");

		service.post("/rack/:to", "addRackDocument");

		service.get("/rack/documents", "listRackDocuments");

		service.get("/rack/:id", "getRackDocument");

		service.delete("/rack/:id", "deleteRackDocument");

		service.post("/rack/documents/copy/:ids/:folder", "copyRackDocuments");

		service.post("/rack/document/copy/:id/:folder", "copyRackDocument");

		service.put("/rack/trash/:id", "moveTrashRack");

		service.get("/rack/documents/Trash", "listRackTrashDocuments");

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(),
				vertx.eventBus(), new WorkspaceResourcesProvider(mongo)));

	}

}
