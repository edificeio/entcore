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

package org.entcore.conversation;

import fr.wseduc.cron.CronTrigger;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.conversation.controllers.ConversationController;
import org.entcore.conversation.service.impl.ConversationRepositoryEvents;
import org.entcore.conversation.service.impl.ConversationStorage;
import org.entcore.conversation.service.impl.DeleteOrphan;

import java.text.ParseException;

import static fr.wseduc.webutils.Utils.getOrElse;

public class Conversation extends BaseServer {

	public final static int DEFAULT_FOLDER_DEPTH = 3;

	@Override
	public void start() throws Exception {
		super.start();

		Storage storage = new StorageFactory(vertx, config, new ConversationStorage()).getStorage();

		final String exportPath = config
				.getString("export-path", System.getProperty("java.io.tmpdir"));

		addController(new ConversationController(storage, exportPath));

		setRepositoryEvents(new ConversationRepositoryEvents(storage, getOrElse(config.getLong("repositoryEventsTimeout"), 300000l),vertx));

		final String deleteOrphanCron = config.getString("deleteOrphanCron");
		if (deleteOrphanCron != null) {
			try {
				new CronTrigger(vertx, deleteOrphanCron).schedule(new DeleteOrphan(storage));
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
	}

}
