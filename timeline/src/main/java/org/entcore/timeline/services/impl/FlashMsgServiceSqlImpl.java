package org.entcore.timeline.services.impl;

import static org.entcore.common.sql.Sql.parseId;
import static org.entcore.common.sql.SqlResult.*;

import java.util.List;

import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.timeline.services.FlashMsgService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.Logger;

import fr.wseduc.webutils.Either;

public class FlashMsgServiceSqlImpl extends SqlCrudService implements FlashMsgService {

	private final String JOIN_TABLE;
	private final Logger log = LoggerFactory.getLogger(FlashMsgServiceSqlImpl.class);

	public FlashMsgServiceSqlImpl(String schema, String table) {
		super(schema, table);
		JOIN_TABLE = schema + ".messages_read";
	}

	@Override
	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		sql.insert(resourceTable, data, validUniqueResultHandler(handler));
	}

	@Override
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler){
		StringBuilder sb = new StringBuilder();
		JsonArray values = new JsonArray();
		for (String attr : data.fieldNames()) {
			if("startDate".equals(attr) || "endDate".equals(attr)){
				sb.append("\"" + attr + "\"").append(" = ?::timestamptz, ");
			} else if ("contents".equals(attr) || "profiles".equals(attr)) {
				sb.append("\"" + attr + "\"").append(" = ?::jsonb, ");
			} else {
				sb.append("\"" + attr + "\"").append(" = ?, ");
			}
			values.add(data.getValue(attr));
		}
		String query =
				"UPDATE " + resourceTable +
				" SET " + sb.toString() + "modified = NOW() " +
				"WHERE id = ? ";
		sql.prepared(query, values.add(parseId(id)), validRowsResultHandler(handler));
	}

	@Override
	public void deleteMultiple(List<String> ids, Handler<Either<String, JsonObject>> handler) {
		String query = "DELETE FROM " + resourceTable + " WHERE id IN " + Sql.listPrepared(ids.toArray());
		JsonArray values = new JsonArray();
		for(String id : ids){
			try {
				long idNb = Long.parseLong(id);
				values.add(idNb);
			} catch (NumberFormatException e) {
				log.error("Bad id - not a number : " + id.toString());
			}
		}

		sql.prepared(query, values, validUniqueResultHandler(handler));
	}

	@Override
	public void list(String domain, Handler<Either<String, JsonArray>> handler) {
		String query =
			"SELECT *, \"startDate\"::text, \"endDate\"::text "+
			"FROM " + resourceTable + " m  WHERE domain = ? ORDER BY modified DESC";

		JsonArray values = new JsonArray().add(domain);
		sql.prepared(query, values, validResultHandler(handler, "contents", "profiles"));
	}

	@Override
	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler) {
		String query = "SELECT id, contents, color, \"customColor\" FROM " + resourceTable + " m " +
			"WHERE contents -> '"+ lang +"' IS NOT NULL " +
			"AND trim(contents ->> '"+ lang +"') <> '' " +
			"AND profiles ? '" + user.getType() + "' " +
			"AND \"startDate\" <= now() " +
			"AND \"endDate\" > now() " +
			"AND domain = '" + domain + "' " +
			"AND NOT EXISTS (SELECT * FROM " + JOIN_TABLE + " WHERE message_id = m.id AND user_id = '"+ user.getUserId() + "') " +
			"ORDER BY modified DESC";

		sql.raw(query, validResultHandler(handler, "contents"));
	}

	@Override
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject()
			.put("message_id", id)
			.put("user_id", user.getUserId());

		sql.insert(JOIN_TABLE, params, validUniqueResultHandler(handler));
	}

}
