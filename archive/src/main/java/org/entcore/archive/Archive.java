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

package org.entcore.archive;

import fr.wseduc.cron.CronTrigger;
import org.entcore.archive.controllers.ArchiveController;
import org.entcore.archive.controllers.ImportController;
import org.entcore.archive.controllers.DuplicationController;
import org.entcore.archive.filters.ArchiveFilter;
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.archive.services.impl.DeleteOldArchives;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.utils.MapFactory;

import java.text.ParseException;
import java.util.Map;

public class Archive extends BaseServer {

	public static final String ARCHIVES = "archives";

	@Override
	public void start() throws Exception {
		setResourceProvider(new ArchiveFilter());
		super.start();

		Storage storage = new StorageFactory(vertx, config).getStorage();

		final Map<String, Long> archiveInProgress = MapFactory.getSyncClusterMap(Archive.ARCHIVES, vertx);

		String importPath = config.getString("import-path", System.getProperty("java.io.tmpdir"));
		ImportService importService = new DefaultImportService(vertx, storage, importPath, null);

		ArchiveController ac = new ArchiveController(storage, archiveInProgress);
		ImportController ic = new ImportController(importService, storage, archiveInProgress);
		DuplicationController dc = new DuplicationController(vertx, storage, importPath);

		addController(ac);
		addController(ic);
		addController(dc);

		String purgeArchivesCron = config.getString("purgeArchive");
		if (purgeArchivesCron != null) {
			try {
				new CronTrigger(vertx, purgeArchivesCron).schedule(
						new DeleteOldArchives(
								new StorageFactory(vertx, config).getStorage(),
								config.getInteger("deleteDelay", 24),
								importService
						));
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
	}

}
