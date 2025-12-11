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

import static org.entcore.common.sql.Sql.parseId;
import static org.entcore.common.sql.SqlResult.*;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.timeline.services.FlashMsgService;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.Logger;

import fr.wseduc.webutils.Either;

public class FlashMsgServiceSqlImpl extends SqlCrudService implements FlashMsgService {

	private final String JOIN_TABLE;
	private final String STRUCT_JOIN_TABLE;
	private final Logger log = LoggerFactory.getLogger(FlashMsgServiceSqlImpl.class);

	public FlashMsgServiceSqlImpl(String schema, String table) {
		super(schema, table);
		JOIN_TABLE = schema + ".messages_read";
		STRUCT_JOIN_TABLE = schema + ".messages_substructures";
	}

	@Override
	public void create(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		sql.insert(resourceTable, data, "id", validUniqueResultHandler(handler));
	}

	@Override
	public void update(String id, String structureId, JsonObject data, Handler<Either<String, JsonObject>> handler){
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
				"WHERE id = ? " + (structureId != null ? "AND \"structureId\" = ? " : "AND \"structureId\" IS NULL ");
		values.add(parseId(id));
		if(structureId != null)
			values.add(structureId);
		sql.prepared(query, values, validRowsResultHandler(handler));
	}
	
	@Override
	public void delete(String id, String structureId, Handler<Either<String, JsonObject>> handler)
	{
		String query = "DELETE FROM " + resourceTable + " WHERE id = ? " + (structureId != null ? "AND \"structureId\" = ? " : "AND \"structureId\" IS NULL ");
		JsonArray values = new JsonArray().add(parseId(id));
		if(structureId != null)
			values.add(structureId);
		sql.prepared(query, values, validRowsResultHandler(handler));
	}

	@Override
	public void deleteMultiple(List<String> ids, String structureId, Handler<Either<String, JsonObject>> handler) {
		String query = "DELETE FROM " + resourceTable + " WHERE " + (structureId != null ? "\"structureId\" = ? " : "\"structureId\" IS NULL ") + " AND id IN " + Sql.listPrepared(ids.toArray());
		JsonArray values = new JsonArray();
		if(structureId != null)
			values.add(structureId);
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
			"FROM " + resourceTable + " m  WHERE domain = ? AND \"structureId\" IS NULL ORDER BY modified DESC";

		JsonArray values = new JsonArray().add(domain);
		sql.prepared(query, values, validResultHandler(handler, "contents", "profiles", "userPositions"));
	}

	@Override
	public void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler) {
		String query =
				"SELECT DISTINCT m.*, m.\"startDate\" AT TIME ZONE 'utc' AS \"startDate\", m.\"endDate\" AT TIME ZONE 'utc' AS \"endDate\" "+
						"FROM " + resourceTable + " m " +
						"LEFT JOIN " + STRUCT_JOIN_TABLE + " messStru ON m.id = messStru.message_id " +
						"WHERE \"structureId\" = ? " +
						"OR messStru.structure_id = ? ORDER BY modified DESC";

		JsonArray values = new JsonArray().add(structureId).add(structureId);
		sql.prepared(query, values, validResultHandler(handler, "contents", "profiles", "userPositions"));
	}

	@Override
	public void listForUser(UserInfos user, String lang, String domain, Handler<Either<String, JsonArray>> handler) {
		getUserPositions(user.getUserId()).onSuccess(myPositions -> {
			String myStructuresIds;
			String myADMLStructuresId;
			boolean isADMLOfOneStructure;
			try {
				myStructuresIds = String.join(",",
						user.getStructures().stream().map(id -> "'" + id + "'").toArray(String[]::new));
				if (myStructuresIds.isEmpty())
					myStructuresIds = "NULL";
			} catch (Exception e) {
				myStructuresIds = "NULL";
			}
			try {
				myADMLStructuresId = String.join(",",
						user.getFunctions().get(ADMIN_LOCAL).getScope().stream().map(id -> "'" + id + "'")
								.toArray(String[]::new));
				if (myADMLStructuresId.isEmpty())
					myADMLStructuresId = "NULL";
			} catch (Exception e) {
				myADMLStructuresId = "NULL";
			}
			try {
				isADMLOfOneStructure = !user.getFunctions().get(ADMIN_LOCAL).getScope().isEmpty();
			} catch (Exception e) {
				isADMLOfOneStructure = false;
			}

			// we don't need to check if the message is in the user's language he has to see it
			// A distinction is made on structureId to disambiguate V1 and V2 and apply domain filter only on V1
			String query = "SELECT id, contents, color, \"customColor\", signature, \"signatureColor\" FROM " + resourceTable + " m " +
					"WHERE (" +
					// Profiles is set and user matches
					"(profiles IS NOT NULL AND jsonb_array_length(profiles) > 0 " +
					"AND (profiles ? '" + user.getType() + "' " +
					"OR (profiles ? 'AdminLocal' AND (" +
					"(\"structureId\" IS NULL AND " + (isADMLOfOneStructure ? "TRUE" : "FALSE") + ") " +
					"OR (\"structureId\" IN (" + myADMLStructuresId + ") " +
					"OR EXISTS (SELECT * FROM " + STRUCT_JOIN_TABLE + " WHERE message_id = m.id AND structure_id IN (" + myADMLStructuresId + "))))))) " +
					// OR UserPositions is set and user matches
					"OR (\"userPositions\" IS NOT NULL AND jsonb_array_length(\"userPositions\") > 0 " +
					"AND EXISTS (SELECT 1 FROM jsonb_array_elements_text(\"userPositions\") AS pos WHERE pos IN (" + myPositions + ")))) " +
					"AND \"startDate\" <= now() " +
					"AND \"endDate\" > now() " +
					"AND ( \"structureId\" IS NOT NULL OR domain = '" + domain + "' )" +
					"AND (\"structureId\" IS NULL " +
					"OR (\"structureId\" IN (" + myStructuresIds + ")) " +
					"OR EXISTS (SELECT * FROM " + STRUCT_JOIN_TABLE + " WHERE message_id = m.id AND structure_id IN (" + myStructuresIds + "))) " +
					"AND NOT EXISTS (SELECT * FROM " + JOIN_TABLE + " WHERE message_id = m.id AND user_id = '" + user.getUserId() + "') " +
					"ORDER BY modified DESC";

			sql.raw(query, validResultHandler(handler, "contents"));
		}).onFailure(err -> {
			log.error("Failed to fetch user positions", err);
			handler.handle(new Either.Left<>("Failed to fetch user positions"));
		});
	}

	private Future<String> getUserPositions(String userId) {
		Promise<String> promise = Promise.promise();

		String neo4jQuery = "MATCH (u:User {id: {userId}})-[:HAS_POSITION]->(p:UserPosition) RETURN p.name as name";
		Map<String, Object> neo4jParams = new HashMap<>();
		neo4jParams.put("userId", userId);

		Neo4j.getInstance().execute(neo4jQuery, neo4jParams, neo4jResult -> {
			String status = neo4jResult.body().getString("status");

			if (!"ok".equals(status)) {
				promise.fail("Failed to query user positions from Neo4j");
				return;
			}

			JsonArray results = neo4jResult.body().getJsonArray("result");
			String myPositions = Optional.ofNullable(results)
					.filter(arr -> !arr.isEmpty())
					.map(arr -> arr.stream()
							.filter(obj -> obj instanceof JsonObject)
							.map(obj -> (JsonObject) obj)
							.filter(position -> position.containsKey("name"))
							.map(position -> position.getString("name"))
							.map(name -> "'" + name.replace("'", "''") + "'")
							.collect(Collectors.joining(",")))
					.orElse("NULL");

			promise.complete(myPositions);
		});

		return promise.future();
	}

	@Override
	public void getSubstructuresByMessageId(String messageId, Handler<Either<String, JsonArray>> handler) {
		String query = "SELECT structure_id FROM " + STRUCT_JOIN_TABLE + " m " +
				"WHERE m.message_id = ?";
		JsonArray values = new JsonArray().add(messageId);
		sql.prepared(query, values, validResultHandler(handler, "contents", "profiles"));
	}

	@Override
	public void setSubstructuresByMessageId(String messageId, String structureId, JsonObject subStructures, Handler<Either<String, JsonArray>> handler) {
        final SqlStatementsBuilder s = new SqlStatementsBuilder();
		String checkStruct = (structureId != null ? "\"structureId\" = '" + structureId + "' " : "\"structureId\" IS NULL ");
		s.raw("DELETE FROM " + STRUCT_JOIN_TABLE + " WHERE message_id IN " +
				"(SELECT id FROM " + resourceTable + " WHERE id = '" + messageId + "' AND " + checkStruct + ")");
        JsonArray subs = subStructures.getJsonArray("subStructures");
        if (subs != null && !subs.isEmpty()) {
            String query = "INSERT INTO " + STRUCT_JOIN_TABLE + " VALUES ";
            for (int i = 0; i < subs.size(); i++) {
                String structure = subs.getString(i);
                query += " ('"+messageId+"','"+structure+"'),";
            }
            s.raw(query.substring(0, query.length()-1));
        }
        sql.transaction(s.build(), validResultHandler(handler));
	}

	@Override
	public void markAsRead(UserInfos user, String id, Handler<Either<String, JsonObject>> handler) {
		String query = "INSERT INTO " + JOIN_TABLE + " " +
				"(message_id, user_id) VALUES (?,?) ON CONFLICT DO NOTHING";
		JsonArray values = new JsonArray().add(id).add(user.getUserId());
		sql.prepared(query, values, validUniqueResultHandler(handler));
	}

	@Override
	public void purgeMessagesRead(Handler<Either<String, JsonObject>> handler) {
		String query = "DELETE FROM " + JOIN_TABLE + " " +
				"WHERE message_id IN (SELECT id FROM " + resourceTable + " " +
					"WHERE " + resourceTable + ".\"endDate\" <= NOW())";
		sql.raw(query, validUniqueResultHandler(handler));
	}

}
