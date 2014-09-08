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

import org.entcore.common.http.BaseServer;
import org.entcore.common.user.RepositoryHandler;
import org.entcore.workspace.controllers.QuotaController;
import org.entcore.workspace.security.WorkspaceResourcesProvider;
import org.entcore.workspace.service.QuotaService;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.WorkspaceRepositoryEvents;
import org.vertx.java.core.http.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;

public class Workspace extends BaseServer {

	@Override
	public void start() {
		setResourceProvider(new WorkspaceResourcesProvider());
		super.start();
		WorkspaceService service = new WorkspaceService();

		String neo4jPluginUri = container.config().getString("neo4jPluginUri");
		HttpClient neo4jPlugin = null;
		if (neo4jPluginUri != null && !neo4jPluginUri.trim().isEmpty()) {
			try {
				URI uri = new URI(neo4jPluginUri);
				neo4jPlugin =  vertx.createHttpClient()
						.setHost(uri.getHost())
						.setPort(uri.getPort())
						.setMaxPoolSize(16)
						.setKeepAlive(false);
			} catch (URISyntaxException e) {
				log.error(e.getMessage(), e);
			}
		}
		final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin);

		String gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		vertx.eventBus().registerHandler("user.repository",
				new RepositoryHandler(new WorkspaceRepositoryEvents(vertx, gridfsAddress,
						config.getBoolean("share-old-groups-to-users", false))));

		service.setQuotaService(quotaService);
		addController(service);

		QuotaController quotaController = new QuotaController();
		quotaController.setQuotaService(quotaService);
		addController(quotaController);

	}

}
