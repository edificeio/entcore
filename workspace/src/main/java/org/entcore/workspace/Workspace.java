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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpServerOptions;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.workspace.controllers.AudioRecorderHandler;
import org.entcore.workspace.controllers.QuotaController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.security.WorkspaceResourcesProvider;
import org.entcore.workspace.service.QuotaService;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.AudioRecorderWorker;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.WorkspaceRepositoryEvents;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.workspace.service.impl.WorkspaceSearchingEvents;

public class Workspace extends BaseServer {

	public static final String REVISIONS_COLLECTION = "documentsRevisions";

	@Override
	public void start() throws Exception {
		setResourceProvider(new WorkspaceResourcesProvider());
		super.start();

		Storage storage = new StorageFactory(vertx, config,
				new MongoDBApplicationStorage("documents", Workspace.class.getSimpleName())).getStorage();
		WorkspaceService service = new WorkspaceService();

		final boolean neo4jPlugin = config.getBoolean("neo4jPlugin", false);
		final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin);

		setRepositoryEvents(new WorkspaceRepositoryEvents(vertx, storage,
						config.getBoolean("share-old-groups-to-users", false)));

		if (config.getBoolean("searching-event", true)) {
			setSearchingEvents(new WorkspaceSearchingEvents(DocumentDao.DOCUMENTS_COLLECTION));
		}

		service.setQuotaService(quotaService);
		service.setStorage(storage);
		addController(service);

		QuotaController quotaController = new QuotaController();
		quotaController.setQuotaService(quotaService);
		addController(quotaController);

		if (config.getInteger("wsPort") != null) {
			vertx.deployVerticle(AudioRecorderWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
			HttpServerOptions options = new HttpServerOptions().setMaxWebsocketFrameSize(1024 * 1024);
			vertx.createHttpServer(options)
					.websocketHandler(new AudioRecorderHandler(vertx)).listen(config.getInteger("wsPort"));
		}

	}

}
