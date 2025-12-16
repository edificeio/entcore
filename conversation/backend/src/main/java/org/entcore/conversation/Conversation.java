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

import java.text.ParseException;

import static org.entcore.common.editor.ContentTransformerConfig.getContentTransformerConfig;

import io.vertx.core.Future;
import org.entcore.common.editor.ContentTransformerEventRecorderFactory;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.conversation.controllers.ApiController;
import org.entcore.conversation.controllers.ConversationController;
import org.entcore.conversation.controllers.TaskController;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.ConversationRepositoryEvents;
import org.entcore.conversation.service.impl.ConversationStorage;
import org.entcore.conversation.service.impl.DeleteOrphan;
import org.entcore.conversation.service.impl.Neo4jConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;

import fr.wseduc.cron.CronTrigger;
import fr.wseduc.transformer.ContentTransformerFactoryProvider;
import fr.wseduc.transformer.IContentTransformerClient;
import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class Conversation extends BaseServer {

	public final static int DEFAULT_FOLDER_DEPTH = 3;
	/** Default delay in minutes after which a message can not be recalled. */
	public final static int DEFAULT_RECALL_DELAY = 60;
	/** Default strategy for getting visible contacts. */
	public final static String DEFAULT_GET_VISIBLE_STRATEGY = "all-at-once"; /* other expected value is "filtered" */

	public final static int DEFAULT_CONVERSATION_BATCH_SIZE = 1000;

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		final Promise<Void> promise = Promise.promise();
		super.start(promise);
		promise.future()
				.compose(init -> StorageFactory.build(vertx, config, new ConversationStorage()))
				.compose(this::initConversation)
				.onComplete(startPromise);
	}

	public Future<Void> initConversation(StorageFactory storageFactory) {
		final Storage storage = storageFactory.getStorage();

		ContentTransformerFactoryProvider.init(vertx);
		final JsonObject contentTransformerConfig = getContentTransformerConfig(vertx).orElse(null);
		final IContentTransformerClient contentTransformerClient = ContentTransformerFactoryProvider.getFactory("conversation", contentTransformerConfig).create();
		final IContentTransformerEventRecorder contentTransformerEventRecorder = new ContentTransformerEventRecorderFactory("conversation", contentTransformerConfig).create();

		final ConversationService conversationService = new SqlConversationService(vertx, config.getString("db-schema", "conversation"), contentTransformerClient, contentTransformerEventRecorder)
				.setSendTimeout(config.getInteger("send-timeout",SqlConversationService.DEFAULT_SENDTIMEOUT));
		final Neo4jConversationService userService = new Neo4jConversationService();

		final String exportPath = config
				.getString("export-path", System.getProperty("java.io.tmpdir"));

		addController(
			new ConversationController(storage, exportPath)
			.setConversationService(conversationService)
			.setUserService(userService)
		);
		addController(
			new ApiController()
			.setConversationService(conversationService)
		);

		setRepositoryEvents(new ConversationRepositoryEvents(storage, getOrElse(config.getLong("repositoryEventsTimeout"), 300000l),vertx));

		// Delete Orphans
		final String deleteOrphanCron = config.getString("deleteOrphanCron");
		final DeleteOrphan deleteOrphan = new DeleteOrphan(storage);
		// Enable delete orphan task to be triggered via API
		addController(new TaskController(deleteOrphan));
		// Schedule delete orphan task from cron expression
		if (deleteOrphanCron != null) {
			try {
				new CronTrigger(vertx, deleteOrphanCron).schedule(deleteOrphan);
			} catch (ParseException e) {
				log.error("Invalid cron expression.", e);
			}
		}
		return Future.succeededFuture();
	}

}
