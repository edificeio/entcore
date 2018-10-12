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
