/*
 * Copyright © "Open Digital Education", 2018
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
 */

package org.entcore.timeline.services.impl;

import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.RepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.webutils.Either;

public class FlashMsgRepositoryEventsSql implements RepositoryEvents {

	private final Sql sql = Sql.getInstance();
	private static final Logger log = LoggerFactory.getLogger(FlashMsgRepositoryEventsSql.class);

	@Override
	public void exportResources(JsonArray resourcesIds, String exportId, String userId,
			JsonArray groups, String exportPath, String locale, String host,
			Handler<Boolean> handler) {}

	@Override
	public void removeShareGroups(JsonArray oldGroups) {
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		String query =
			"DELETE FROM flashmsg.messages_read r "+
			"USING flashmsg.messages m " +
			"WHERE message_id = id AND \"endDate\" < now()";

		sql.raw(query, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					log.error("[FlashMsg] Error deleting flash messages read marks on deleteGroups event.");
					return;
				}
			}
		}));
	}

	@Override
	public void deleteUsers(JsonArray users) {}

}
