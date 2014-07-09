/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.workspace;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Server;
import org.entcore.common.http.filter.ActionFilter;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.workspace.security.WorkspaceResourcesProvider;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.WorkspaceRepositoryEvents;
import org.vertx.java.core.Future;

public class Workspace extends Server {

	@Override
	public void start(final Future<Void> result) {
		super.start();

		final MongoDb mongo = MongoDb.getInstance();
		mongo.init(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));

		String gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		vertx.eventBus().registerHandler("user.repository",
				new RepositoryHandler(new WorkspaceRepositoryEvents(vertx, gridfsAddress)));

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

		try {
			service.registerMethod("org.entcore.workspace", "workspaceEventBusHandler");
		} catch (NoSuchMethodException | IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(),
				Server.getEventBus(vertx), new WorkspaceResourcesProvider(mongo)));

	}

}
