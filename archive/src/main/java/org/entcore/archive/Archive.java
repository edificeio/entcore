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
import fr.wseduc.webutils.collections.SharedDataHelper;
import fr.wseduc.webutils.security.RSA;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;

import org.apache.commons.lang3.tuple.Pair;
import org.entcore.archive.controllers.*;
import org.entcore.archive.filters.ArchiveFilter;
import org.entcore.archive.services.ImportService;
import org.entcore.archive.services.RepriseService;
import org.entcore.archive.services.impl.*;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;

public class Archive extends BaseServer {

	public static final String ARCHIVES = "archives";

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final Promise<Void> promise = Promise.promise();
		super.start(promise);
		promise.future()
				.compose(init -> StorageFactory.build(vertx, config))
				.compose(storageFactory -> SharedDataHelper.getInstance().<String, String>getLocalAsyncMap("server")
						.map(archiveConfigMap -> Pair.of(storageFactory, archiveConfigMap)))
				.compose(configPair -> initArchives(configPair.getLeft(), configPair.getRight()))
				.onComplete(startPromise);
	}

	public Future<Void> initArchives(final StorageFactory storageFactory, final AsyncMap<String, String> archivesMap){
		setDefaultResourceFilter(new ArchiveFilter());
		Storage storage = storageFactory.getStorage();

		Integer storageTimeout = config.getInteger("import-storage-timeout", 600);
		String exportPath = config.getString("export-path", System.getProperty("java.io.tmpdir"));
		String importPath = config.getString("import-path", System.getProperty("java.io.tmpdir"));
		String privateKeyPath = config.getString("archive-private-key", null);
		boolean forceEncryption = config.getBoolean("force-encryption", false); //TODO: Set the default to true when it is safe to do so

		archivesMap.put("archiveConfig", new JsonObject().put("storageTimeout", storageTimeout).encode());

		PrivateKey signKey;
		PublicKey verifyKey;
		try {
			signKey = RSA.loadPrivateKey(vertx, privateKeyPath);
			verifyKey = RSA.loadPublicKey(vertx, privateKeyPath);
		} catch (Exception e) {
			return Future.failedFuture(e);
		}

		ImportService importService = new DefaultImportService(vertx, config, storage, importPath, null, verifyKey, forceEncryption);

		ArchiveController ac = new ArchiveController(storage, signKey, forceEncryption);
		ImportController ic = new ImportController(importService, storage);
		DuplicationController dc = new DuplicationController(vertx, storage, importPath, signKey, verifyKey, forceEncryption);

		addController(ac);
		addController(ic);
		addController(dc);

		DeleteOldArchivesTask deleteOldArchivesTask = new DeleteOldArchivesTask(vertx,
				storageFactory.getStorage(),
				config.getInteger("deleteDelay", 24),
				exportPath,
				importService,
				importPath,
				config.getBoolean("enablePurgeByFileAge", true),
				config.getInteger("maxFileAge", 24)
		);

		String purgeArchivesCron = config.getString("purgeArchive");
		if (purgeArchivesCron != null) {
			try {
				new CronTrigger(vertx, purgeArchivesCron).schedule(deleteOldArchivesTask);
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
		RepriseExportTask repriseExportTask = new RepriseExportTask(teacherPersonnelFirst, repriseService);
		String repriseExportCron = reprise.getString("export-cron");
		if (repriseExportCron != null) {
			try {
				new CronTrigger(vertx, repriseExportCron).schedule(repriseExportTask);
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
		RepriseImportTask repriseImportTask = new RepriseImportTask(teacherPersonnelFirst, repriseService);
		String repriseImportCron = reprise.getString("import-cron");
		if (repriseImportCron != null) {
			try {
				new CronTrigger(vertx, repriseImportCron).schedule(repriseImportTask);
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}

		// Add controller to trigger the tasks via API
		addController(new TaskController(deleteOldArchivesTask, repriseExportTask, repriseImportTask));

		return Future.succeededFuture();
	}

}
