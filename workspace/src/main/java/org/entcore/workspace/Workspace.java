/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.workspace;

import java.util.HashMap;

import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.share.impl.GenericShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.workspace.controllers.AudioRecorderHandler;
import org.entcore.workspace.controllers.QuotaController;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.security.WorkspaceResourcesProvider;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.AudioRecorderWorker;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.DefaultWorkspaceService;
import org.entcore.workspace.service.impl.WorkspaceRepositoryEvents;
import org.entcore.workspace.service.impl.WorkspaceSearchingEvents;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpServerOptions;

public class Workspace extends BaseServer {

	public static final String REVISIONS_COLLECTION = "documentsRevisions";

	@Override
	public void start() throws Exception {
		WorkspaceResourcesProvider resourceProvider = new WorkspaceResourcesProvider();
		setResourceProvider(resourceProvider);
		super.start();

		Storage storage = new StorageFactory(vertx, config,
				new MongoDBApplicationStorage(DocumentDao.DOCUMENTS_COLLECTION, Workspace.class.getSimpleName())).getStorage();

		final boolean neo4jPlugin = config.getBoolean("neo4jPlugin", false);
		final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin,
				new TimelineHelper(vertx, vertx.eventBus(), config));

		/**
		 * SHare Service
		 */
		GenericShareService shareService = new MongoDbShareService(vertx.eventBus(), MongoDb.getInstance(),
				DocumentDao.DOCUMENTS_COLLECTION, securedActions, new HashMap<>());
		final int threshold = config.getInteger("alertStorage", 80);
		/**
		 * Folder manager
		 */
		FolderManager folderManagerWithQuota = FolderManager.mongoManagerWithQuota(DocumentDao.DOCUMENTS_COLLECTION, storage,
				vertx, shareService, quotaService, threshold);
		resourceProvider.setFolderManager(folderManagerWithQuota);
		/**
		 * Repo events
		 */
		FolderManager folderManagerRevision = FolderManager.mongoManagerWithQuota(REVISIONS_COLLECTION, storage, vertx,
				shareService, quotaService, threshold);
		boolean shareOldGroups = config.getBoolean("share-old-groups-to-users", false);
		setRepositoryEvents(
				new WorkspaceRepositoryEvents(vertx, storage, shareOldGroups, folderManagerWithQuota, folderManagerRevision));

		/**
		 * SearchEvent
		 */
		if (config.getBoolean("searching-event", true)) {
			setSearchingEvents(new WorkspaceSearchingEvents(folderManagerWithQuota));
		}
		/**
		 * Controllers
		 */
		String node = (String) vertx.sharedData().getLocalMap("server").get("node");
		if (node == null) {
			node = "";
		}
		FolderManager folderManager = FolderManager.mongoManager(DocumentDao.DOCUMENTS_COLLECTION, storage, vertx, shareService);
		String imageResizerAddress = node + config.getString("image-resizer-address", "wse.image.resizer");
		WorkspaceService workspaceService = new DefaultWorkspaceService(storage, MongoDb.getInstance(), threshold,
				imageResizerAddress, quotaService, folderManager, vertx.eventBus(), shareService);

		final PdfGenerator pdfGenerator = new PdfFactory(vertx, config).getPdfGenerator();

		WorkspaceController workspaceController = new WorkspaceController(storage, workspaceService, shareService,pdfGenerator, MongoDb.getInstance());
		addController(workspaceController);
		//

		QuotaController quotaController = new QuotaController();
		quotaController.setQuotaService(quotaService);
		addController(quotaController);

		if (config.getInteger("wsPort") != null) {
			vertx.deployVerticle(AudioRecorderWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
			HttpServerOptions options = new HttpServerOptions().setMaxWebsocketFrameSize(1024 * 1024);
			vertx.createHttpServer(options).websocketHandler(new AudioRecorderHandler(vertx))
					.listen(config.getInteger("wsPort"));
		}

	}

}
