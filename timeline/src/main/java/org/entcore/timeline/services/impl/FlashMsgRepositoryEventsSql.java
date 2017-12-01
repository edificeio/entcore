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
	public void exportResources(String exportId, String userId,
			JsonArray groups, String exportPath, String locale, String host,
			Handler<Boolean> handler) {}

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
