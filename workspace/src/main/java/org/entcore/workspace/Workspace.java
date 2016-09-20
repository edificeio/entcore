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

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.webutils.Either;
import org.entcore.common.http.BaseServer;
import org.entcore.common.service.impl.MongoDbSearchService;
import org.entcore.workspace.controllers.QuotaController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.security.WorkspaceResourcesProvider;
import org.entcore.workspace.service.QuotaService;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.WorkspaceRepositoryEvents;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.workspace.service.impl.WorkspaceSearchingEvents;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.text.ParseException;

public class Workspace extends BaseServer {

	public static final String REVISIONS_COLLECTION = "documentsRevisions";
	private QuotaService quotaService;

	@Override
	public void start() {
		setResourceProvider(new WorkspaceResourcesProvider());
		super.start();

		Storage storage = new StorageFactory(vertx, config).getStorage();
		WorkspaceService service = new WorkspaceService();

		final boolean neo4jPlugin = container.config().getBoolean("neo4jPlugin", false);
		final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin);

		// cron for structure quota update
		// by default, at 2:00AM every day
		final String updateStructureQuota = container.config().getString("update-structure-quota", " 0 0 2 ? * * *");
		try{
			new CronTrigger(vertx, updateStructureQuota).schedule(new Handler<Long>() {
				@Override
				public void handle(Long event) {
					quotaService.updateStructureStorageInitialize(new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> r) {
							if (r.isRight()) {
								quotaService.updateStructureStorage(new Handler<Either<String, JsonObject>>() {
									@Override
									public void handle(Either<String, JsonObject> r) {
										if (r.isLeft()) {
											JsonObject error = new JsonObject()
													.putString("error", r.left().getValue());
										}
									}
								});
							}
						}
					});
				}
			});
		} catch (ParseException e) {
			log.error("Failed to start structure quota crons.");
			return;
		}


	setRepositoryEvents(new WorkspaceRepositoryEvents(vertx, storage,
						config.getBoolean("share-old-groups-to-users", false)));

		if (config.getBoolean("searching-event", true)) {
			//Denormalizing : don't use the owner object
			setSearchingEvents(new WorkspaceSearchingEvents(DocumentDao.DOCUMENTS_COLLECTION,
					new MongoDbSearchService(DocumentDao.DOCUMENTS_COLLECTION, "owner")));
		}

		service.setQuotaService(quotaService);
		service.setStorage(storage);
		addController(service);

		QuotaController quotaController = new QuotaController();
		quotaController.setQuotaService(quotaService);
		addController(quotaController);

	}

}
