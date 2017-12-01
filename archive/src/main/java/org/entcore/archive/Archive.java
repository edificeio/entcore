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

package org.entcore.archive;

import fr.wseduc.cron.CronTrigger;
import org.entcore.archive.controllers.ArchiveController;
import org.entcore.archive.filters.ArchiveFilter;
import org.entcore.archive.services.impl.DeleteOldExports;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.StorageFactory;

import java.text.ParseException;

public class Archive extends BaseServer {

	public static final String ARCHIVES = "archives";

	@Override
	public void start() throws Exception {
		setResourceProvider(new ArchiveFilter());
		super.start();
		addController(new ArchiveController());

		String purgeArchivesCron = config.getString("purgeArchive");
		if (purgeArchivesCron != null) {
			try {
				new CronTrigger(vertx, purgeArchivesCron).schedule(
						new DeleteOldExports(
								new StorageFactory(vertx, config).getStorage(),
								config.getInteger("deleteDelay", 24)
						));
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
	}

}
