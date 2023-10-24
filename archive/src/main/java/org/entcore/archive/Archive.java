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
import fr.wseduc.webutils.security.RSA;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

import org.entcore.archive.controllers.ArchiveController;
import org.entcore.archive.controllers.ImportController;
import org.entcore.archive.controllers.DuplicationController;
import org.entcore.archive.controllers.RepriseController;
import org.entcore.archive.filters.ArchiveFilter;
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.RepriseService;
import org.entcore.archive.services.impl.DefaultImportService;
import org.entcore.archive.services.impl.DefaultRepriseService;
import org.entcore.archive.services.impl.DeleteOldArchives;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.utils.MapFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Map;

public class Archive extends BaseServer {

	public static final String ARCHIVES = "archives";

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		setResourceProvider(new ArchiveFilter());
		super.start(startPromise);

		Storage storage = new StorageFactory(vertx, config).getStorage();

		final Map<String, Long> archiveInProgress = MapFactory.getSyncClusterMap(Archive.ARCHIVES, vertx);
		final LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");

		Integer storageTimeout = config.getInteger("import-storage-timeout", 600);
		String exportPath = config.getString("export-path", System.getProperty("java.io.tmpdir"));
		String importPath = config.getString("import-path", System.getProperty("java.io.tmpdir"));
		String privateKeyPath = config.getString("archive-private-key", null);
		boolean forceEncryption = config.getBoolean("force-encryption", false); //TODO: Set the default to true when it is safe to do so

		serverMap.put("archiveConfig", new JsonObject().put("storageTimeout", storageTimeout).encode());

		PrivateKey signKey = RSA.loadPrivateKey(vertx, privateKeyPath);
		PublicKey verifyKey = RSA.loadPublicKey(vertx, privateKeyPath);

		ImportService importService = new DefaultImportService(vertx, config, storage, importPath, null, verifyKey, forceEncryption);

		ArchiveController ac = new ArchiveController(storage, archiveInProgress, signKey, forceEncryption);
		ImportController ic = new ImportController(importService, storage, archiveInProgress);
		DuplicationController dc = new DuplicationController(vertx, storage, importPath, signKey, verifyKey, forceEncryption);

		addController(ac);
		addController(ic);
		addController(dc);

		String purgeArchivesCron = config.getString("purgeArchive");
		if (purgeArchivesCron != null) {
			try {
				new CronTrigger(vertx, purgeArchivesCron).schedule(
						new DeleteOldArchives(vertx,
								new StorageFactory(vertx, config).getStorage(),
								config.getInteger("deleteDelay", 24),
								exportPath,
								importService,
								importPath,
								config.getBoolean("enablePurgeByFileAge", true),
								config.getInteger("maxFileAge", 24)
						));
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}

		JsonObject reprise = config.getJsonObject("reprise", new JsonObject());
		String reprisePath = reprise.getString("path", System.getProperty("java.io.tmpdir"));
		ImportService repriseImportService = new DefaultImportService(vertx, config, storage, reprisePath, "reprise:import", verifyKey, forceEncryption);
		RepriseService repriseService = new DefaultRepriseService(vertx, storage, reprise, config, repriseImportService);

		RepriseController rc = new RepriseController(repriseService);
		addController(rc);

		Boolean teacherPersonnelFirst = reprise.getBoolean("teacher-personnel-first", false);
		String repriseExportCron = reprise.getString("export-cron");
		if (repriseExportCron != null) {
			try {
				new CronTrigger(vertx, repriseExportCron).schedule(event -> {
					repriseService.launchExportForUsersFromOldPlatform(teacherPersonnelFirst.booleanValue());
				});
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
		String repriseImportCron = reprise.getString("import-cron");
		if (repriseImportCron != null) {
			try {
				new CronTrigger(vertx, repriseImportCron).schedule(event -> {
					repriseService.launchImportForUsersFromOldPlatform(teacherPersonnelFirst.booleanValue());
				});
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
	}

}
