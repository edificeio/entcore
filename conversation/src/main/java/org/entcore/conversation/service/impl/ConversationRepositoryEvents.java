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

package org.entcore.conversation.service.impl;

import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.RepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import fr.wseduc.webutils.Either;

public class ConversationRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(ConversationRepositoryEvents.class);
	private final Sql sql = Sql.getInstance();
	private final Storage storage;

	public ConversationRepositoryEvents(Storage storage) {
		this.storage = storage;
	}

	@Override
	public void exportResources(String exportId, String userId, JsonArray groups, String exportPath,
			String locale, String host, Handler<Boolean> handler) {

	}

	@Override
	public void deleteGroups(JsonArray groups) {
		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$ $' || ? || '$ ') - (? || '$ $' || ? || '$' || ?) " +
			"WHERE \"to\" @> (?)::jsonb";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$ $' || ? || '$ ') - (? || '$ $' || ? || '$' || ?) " +
			"WHERE \"cc\" @> (?)::jsonb";

		for (Object o : groups) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject group = (JsonObject) o;

			JsonArray params = new JsonArray();

			params.add(group.getString("group", ""));
			params.add(new JsonArray().add(group.getString("groupName", "")).toString());
			params.add(group.getString("group", ""));
			params.add(group.getString("groupName", ""));
			params.add(group.getString("group", ""));
			params.add(group.getString("groupName", ""));
			params.add(group.getString("groupName", ""));
			params.add(new JsonArray().add(group.getString("group", "")).toString());

			builder.prepared(setTO, params);
			builder.prepared(setCC, params);
		}
		sql.transaction(builder.build(), new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error updating delete groups in conversation : " + event.body().encode());
				}
			}
		});
	}

	@Override
	public void deleteUsers(JsonArray users) {
		JsonArray userIds = new JsonArray();
		for (Object o : users) {
			if (!(o instanceof JsonObject)) continue;
			userIds.add(((JsonObject) o).getString("id"));
		}

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String unusedAttachments =
			"WITH unusedAtts AS (" +
				"SELECT DISTINCT attachment_id AS id FROM conversation.usermessagesattachments uma " +
				"GROUP BY attachment_id " +
				"HAVING every(user_id IN "+ Sql.listPrepared(userIds.getList()) +") " +
			") SELECT " +
			"CASE WHEN COUNT(id) = 0 THEN '[]' ELSE json_agg(distinct id) END AS attachmentIds "+
			"FROM unusedAtts u";
		builder.prepared(unusedAttachments, userIds);

		String deleteFolder =
			"DELETE FROM conversation.folders f " +
			"WHERE f.user_id IN " + Sql.listPrepared(userIds.getList());
		builder.prepared(deleteFolder, userIds);

		String deleteUserMessages =
			"DELETE FROM conversation.usermessages um " +
			"WHERE um.user_id IN " + Sql.listPrepared(userIds.getList());
		builder.prepared(deleteUserMessages, userIds);

		String setFrom =
			"UPDATE conversation.messages " +
			"SET " +
			"\"from\" = '', " +
			"\"fromName\" = ?, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"from\" = ?";

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"to\" @> (?)::jsonb";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb, " +
			"\"displayNames\" = \"displayNames\" - (? || '$' || ? || '$ $ ') " +
			"WHERE \"cc\" @> (?)::jsonb";

		for (Object o : users) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject user = (JsonObject) o;
			JsonArray paramsToCc = new JsonArray();
			JsonArray paramsFrom = new JsonArray();

			paramsToCc.add(user.getString("id", ""));
			paramsToCc.add(new JsonArray().add(user.getString("displayName", "")).toString());
			paramsToCc.add(user.getString("id", ""));
			paramsToCc.add(user.getString("displayName", ""));
			paramsToCc.add(new JsonArray().add(user.getString("id", "")).toString());

			paramsFrom.add(user.getString("displayName", ""));
			paramsFrom.add(user.getString("id", ""));
			paramsFrom.add(user.getString("displayName", ""));
			paramsFrom.add(user.getString("id", ""));

			builder.prepared(setTO, paramsToCc);
			builder.prepared(setCC, paramsToCc);
			builder.prepared(setFrom, paramsFrom);
		}
		sql.transaction(builder.build(), SqlResult.validResultsHandler(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					log.error("Error deleting conversation data : " + event.left().getValue());
					return;
				}

				JsonArray results = event.right().getValue();
				JsonArray attachmentIds =
					results.getJsonArray(0).size() > 0 ?
						new JsonArray(results.getJsonArray(0).getJsonObject(0).getString("attachmentIds", "[]")) :
						new JsonArray();

				for(Object attachmentObj: attachmentIds){
					final String attachmentId = (String) attachmentObj;
					storage.removeFile(attachmentId, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if (!"ok".equals(event.getString("status"))) {
								log.error("["+ConversationRepositoryEvents.class.getSimpleName()+"] Error while tying to delete attachment file (_id: {"+attachmentId+"})");
							}
						}
					});
				}
			}
		}, "attachmentIds"));
	}

}
