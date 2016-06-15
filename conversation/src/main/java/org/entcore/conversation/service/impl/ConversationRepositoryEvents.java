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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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

		String setDisplayNames =
			"WITH message AS(" +
			"    SELECT m.* " +
			"    FROM conversation.messages AS m " +
			"    WHERE \"displayNames\"::text LIKE '%' || ? || '%' LIMIT 1" +
			"), elts AS (" +
			"    SELECT jsonb_array_elements_text(\"displayNames\") AS elt FROM message" +
			"), fullGroupTxt AS (" +
			"    SELECT elt FROM elts " +
			"    WHERE elt LIKE ? || '%'" +
			") " +
			"UPDATE conversation.messages " +
			"SET " +
			"\"displayNames\" = \"displayNames\" - (SELECT elt FROM fullGroupTxt) " +
			"WHERE \"displayNames\"::text LIKE '%' || ? || '%'";

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb " +
			"WHERE \"to\" @> ?";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb " +
			"WHERE \"cc\" @> ?";

		for (Object o : groups) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject group = (JsonObject) o;

			JsonArray params1 = new JsonArray();
			JsonArray params2 = new JsonArray();

			for(int i = 0; i < 3; i++)
				params1.add(group.getString("group", ""));

			params2.add(group.getString("group", ""));
			params2.add(new JsonArray().add(group.getString("groupName", "")).toString());
			params2.add(new JsonArray().add(group.getString("group", "")).toString());

			builder.prepared(setDisplayNames, params1);
			builder.prepared(setTO, params2);
			builder.prepared(setCC, params2);
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
			userIds.addString(((JsonObject) o).getString("id"));
		}

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String unusedAttachments =
			"WITH unusedAtts AS (" +
				"SELECT DISTINCT attachment_id AS id FROM conversation.usermessagesattachments uma " +
				"GROUP BY attachment_id " +
				"HAVING every(user_id IN "+ Sql.listPrepared(userIds.toArray()) +") " +
			") SELECT " +
			"CASE WHEN COUNT(id) = 0 THEN '[]' ELSE json_agg(distinct id) END AS attachmentIds "+
			"FROM unusedAtts u";
		builder.prepared(unusedAttachments, userIds);

		String deleteFolder =
			"DELETE FROM conversation.folders f " +
			"WHERE f.user_id IN " + Sql.listPrepared(userIds.toArray());
		builder.prepared(deleteFolder, userIds);

		String deleteUserMessages =
			"DELETE FROM conversation.usermessages um " +
			"WHERE um.user_id IN " + Sql.listPrepared(userIds.toArray());
		builder.prepared(deleteUserMessages, userIds);

		String setDisplayNames =
			"WITH message AS(" +
			"    SELECT m.* " +
			"    FROM conversation.messages AS m " +
			"    WHERE \"displayNames\"::text LIKE '%' || ? || '%' LIMIT 1" +
			"), elts AS (" +
			"    SELECT jsonb_array_elements_text(\"displayNames\") AS elt FROM message" +
			"), fullUserTxt AS (" +
			"    SELECT elt FROM elts " +
			"    WHERE elt LIKE ? || '%'" +
			") " +
			"UPDATE conversation.messages " +
			"SET " +
			"\"displayNames\" = \"displayNames\" - (SELECT elt FROM fullUserTxt) " +
			"WHERE \"displayNames\"::text LIKE '%' || ? || '%'";

		String setFrom =
			"UPDATE conversation.messages " +
			"SET " +
			"\"from\" = '', " +
			"\"fromName\" = ? " +
			"WHERE \"from\" = ?";

		String setTO =
			"UPDATE conversation.messages " +
			"SET " +
			"\"to\" = \"to\" - ?, " +
			"\"toName\" = COALESCE(\"toName\", '[]')::jsonb || (?)::jsonb " +
			"WHERE \"to\" @> ?";

		String setCC =
			"UPDATE conversation.messages " +
			"SET " +
			"\"cc\" = \"cc\" - ?, " +
			"\"ccName\" = COALESCE(\"ccName\", '[]')::jsonb || (?)::jsonb " +
			"WHERE \"cc\" @> ?";

		for (Object o : users) {
			if (!(o instanceof JsonObject)) continue;
			JsonObject user = (JsonObject) o;
			JsonArray params1 = new JsonArray();
			JsonArray params2 = new JsonArray();
			JsonArray params3 = new JsonArray();

			for(int i = 0; i < 3; i++)
				params1.add(user.getString("id", ""));

			params2.add(user.getString("id", ""));
			params2.add(new JsonArray().add(user.getString("displayName", "")).toString());
			params2.add(new JsonArray().add(user.getString("id", "")).toString());

			params3.add(user.getString("id", ""));
			params3.add(user.getString("displayName", ""));

			builder.prepared(setDisplayNames, params1);
			builder.prepared(setTO, params2);
			builder.prepared(setCC, params2);
			builder.prepared(setFrom, params3);
		}
		sql.transaction(builder.build(), SqlResult.validResultsHandler(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					log.error("Error deleting conversation data : " + event.left().getValue());
					return;
				}

				JsonArray results = event.right().getValue();
				JsonArray attachmentIds =
					((JsonArray) results.get(0)).size() > 0 ?
						new JsonArray(((JsonObject) ((JsonArray) results.get(0)).get(0)).getString("attachmentIds", "[]")) :
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
