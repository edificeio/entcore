/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.conversation.service.impl;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.user.UserUtils.findVisibles;

import java.util.List;
import java.util.UUID;

import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.validation.StringValidation;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.Utils;

public class SqlConversationService implements ConversationService{

	private final EventBus eb;
	private final Sql sql;

	private final int maxFolderDepth;

	private final String messageTable;
	private final String folderTable;
	private final String attachmentTable;
	private final String userMessageTable;
	private final String userMessageAttachmentTable;

	public SqlConversationService(Vertx vertx, String schema) {
		this.eb = Server.getEventBus(vertx);
		this.sql = Sql.getInstance();
		this.maxFolderDepth = Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH);
		messageTable = schema + ".messages";
		folderTable = schema + ".folders";
		attachmentTable = schema + ".attachments";
		userMessageTable = schema + ".usermessages";
		userMessageAttachmentTable = schema + ".usermessagesattachments";
	}

	@Override
	public void saveDraft(String parentMessageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		save(parentMessageId, message, user, result);
	}

	private void save(String parentMessageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result){
		message
			.putString("id", UUID.randomUUID().toString())
			.putString("from", user.getUserId())
			.putNumber("date", System.currentTimeMillis())
			.putString("state", State.DRAFT.name());

		JsonObject m = Utils.validAndGet(message, MESSAGE_FIELDS, DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		if(parentMessageId != null)
			message.putString("parent_id", parentMessageId);

		// 1 - Insert message
		builder.insert(messageTable, message, "id");

		// 2 - Link message to the user
		builder.insert(userMessageTable, new JsonObject()
			.putString("user_id", user.getUserId())
			.putString("message_id", message.getString("id")));

		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void updateDraft(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		update(messageId, message, user, result);
	}

	private void update(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		message.putNumber("date", System.currentTimeMillis());
		JsonObject m = Utils.validAndGet(message, UPDATE_DRAFT_FIELDS, UPDATE_DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result, messageId))
			return;

		StringBuilder sb = new StringBuilder();
		JsonArray values = new JsonArray();

		for (String attr : message.getFieldNames()) {
			if("to".equals(attr) || "cc".equals(attr) || "displayNames".equals(attr)){
				sb.append("\"" + attr+ "\"").append(" = CAST(? AS JSONB),");
			} else {
				sb.append("\"" + attr+ "\"").append(" = ?,");
			}
			values.add(message.getValue(attr));
		}
		if(sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);

		String query =
			"UPDATE " + messageTable +
			" SET " + sb.toString() + " " +
			"WHERE id = ? AND state = ?";
		values.add(messageId).add("DRAFT");

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	private void getSenderAttachments(String senderId, String messageId, Handler<Either<String, JsonObject>> handler){
		String query =
			"SELECT " +
				"coalesce(json_agg(distinct att.id), '[]'::json) as attachmentIds," +
				"coalesce(sum(att.size), 0)::integer as totalQuota " +
			"FROM " + attachmentTable + " att " +
			"JOIN " + userMessageAttachmentTable + " uma " +
				"ON (att.id = uma.attachment_id) " +
			"JOIN " + userMessageTable + " um " +
				"ON um.user_id = uma.user_id AND um.message_id = uma.message_id " +
			"WHERE um.user_id = ? AND um.message_id = ?";

		sql.prepared(query, new JsonArray().add(senderId).add(messageId), SqlResult.validUniqueResultHandler(handler, "attachmentids"));
	}

	@Override
	public void send(final String parentMessageId, final String draftId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		sendMessage(parentMessageId, draftId, message, user, result);
	}

	private void sendMessage(final String parentMessageId, final String draftId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, draftId))
			return;

		getSenderAttachments(user.getUserId(), draftId, new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					result.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}

				JsonArray attachmentIds = event.right().getValue().getArray("attachmentids");
				long totalQuota = event.right().getValue().getLong("totalquota");

				final JsonArray ids = message.getArray("allUsers", new JsonArray());

				SqlStatementsBuilder builder = new SqlStatementsBuilder();

				String updateMessage =
					"UPDATE " + messageTable + " SET state = ? WHERE id = ? "+
					"RETURNING id, subject";
				builder.prepared(updateMessage, new JsonArray().add("SENT").add(draftId));

				for(Object toObj : ids){
					if(toObj.equals(user.getUserId()))
						continue;

					builder.insert(userMessageTable, new JsonObject()
						.putString("user_id", toObj.toString())
						.putString("message_id", draftId)
						.putNumber("total_quota", totalQuota)
					);
					for(Object attachmentId : attachmentIds){
						builder.insert(userMessageAttachmentTable, new JsonObject()
							.putString("user_id", toObj.toString())
							.putString("message_id", draftId)
							.putString("attachment_id", attachmentId.toString())
						);
					}
				}

				sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
			}
		});
	}

	@Override
	public void list(String folder, String restrain, UserInfos user, int page, Handler<Either<String, JsonArray>> results) {
		int skip = page * LIST_LIMIT;

		String additionalWhere = "";
		JsonArray values = new JsonArray()
			.add(user.getUserId());

		if(restrain != null){
			additionalWhere = "AND um.folder_id = ? AND um.trashed = false";
			values.add(folder);
		} else {
			additionalWhere = addFolderCondition(folder, values, user.getUserId());
		}

		String query = "SELECT m.*, um.unread as unread, " +
			"CASE when COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
			"FROM " + userMessageTable + " um LEFT JOIN " +
			userMessageAttachmentTable + " uma ON um.user_id = uma.user_id AND um.message_id = uma.message_id JOIN " +
			messageTable + " m ON um.message_id = m.id LEFT JOIN " +
			attachmentTable + " att ON uma.attachment_id = att.id " +
			"WHERE um.user_id = ? " + additionalWhere + " " +
			"GROUP BY m.id, unread " +
			"ORDER BY m.date DESC LIMIT " + LIST_LIMIT + " OFFSET " + skip;

		sql.prepared(query, values, SqlResult.validResultHandler(results, "attachments", "to", "toName", "cc", "ccName", "displayNames"));
	}

	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;

		JsonArray values = new JsonArray();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " " +
			"SET trashed = true " +
			"WHERE trashed = false AND user_id = ? AND message_id IN (");

		values.add(user.getUserId());

		for(String id : messagesId){
			query.append("?,");
			values.add(id);
		}
		if(messagesId.size() > 0)
			query.deleteCharAt(query.length() - 1);
		query.append(")");

		sql.prepared(query.toString(), values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result)) return;

		JsonArray values = new JsonArray();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " " +
			"SET trashed = false " +
			"WHERE trashed = true AND user_id = ? AND message_id IN ");

		values.add(user.getUserId());

		query.append(generateInVars(messagesId, values));

		sql.prepared(query.toString(), values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void delete(List<String> messagesId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonArray values2 = new JsonArray();
		JsonArray values3 = new JsonArray();
		values2.add(user.getUserId());
		values3.add(user.getUserId());

		String getTotalQuota =
			"SELECT coalesce(sum(um.total_quota), 0)::integer AS totalQuota FROM " + userMessageTable + " um " +
			"WHERE um.user_id = ? AND um.trashed = true AND um.message_id IN ";

		String deleteUserMessages =
			"DELETE FROM " + userMessageTable + " um " +
			"WHERE um.user_id = ? AND um.trashed = true AND um.message_id IN ";

		getTotalQuota += (generateInVars(messagesId, values2));
		deleteUserMessages += (generateInVars(messagesId, values3));

		builder.prepared(getTotalQuota, values2);
		builder.prepared(deleteUserMessages, values3);

		sql.transaction(builder.build(), SqlResult.validResultsHandler(result));
	}

	@Override
	public void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String updateQuery = "UPDATE " + userMessageTable + " " +
			"SET unread = false " +
			"WHERE user_id = ? AND message_id = ? ";

		String selectQuery =
			"SELECT " +
				"m.*, " +
				"CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
			"FROM " + messageTable + " m " +
			"JOIN " + userMessageTable + " um " +
				"ON m.id = um.message_id " +
			"LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
			"LEFT JOIN " + attachmentTable + " att " +
				"ON att.id = uma.attachment_id " +
			"WHERE um.user_id = ? AND m.id = ?  " +
			"GROUP BY m.id";

		JsonArray values = new JsonArray()
			.add(user.getUserId())
			.add(messageId);

		builder.prepared(updateQuery, values);
		builder.prepared(selectQuery, values);

		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(1, result, "attachments", "to", "toName", "cc", "ccName", "displayNames"));
	}

	@Override
	public void count(String folder, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folder))
			return;

		JsonArray values = new JsonArray()
			.add(user.getUserId());

		String query = "SELECT count(*) as count FROM " + userMessageTable + " um JOIN " +
			messageTable + " m ON (um.message_id = m.id) " +
			"WHERE user_id = ? ";

		if(unread != null){
			query += "AND unread = ? ";
			values.add(unread);
		}

		query += addFolderCondition(folder, values, user.getUserId());

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void findVisibleRecipients(final String parentMessageId, final UserInfos user,
			final String acceptLanguage, final String search, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;

		final JsonObject visible = new JsonObject();

		final JsonObject params = new JsonObject();

		final String preFilter;
		if (isNotEmpty(search)) {
			preFilter = "AND m.displayNameSearchField CONTAINS {search} ";
			params.putString("search", StringValidation.removeAccents(search.trim()).toLowerCase());
		} else {
			preFilter = null;
		}

		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			String getMessageQuery = "SELECT m.* FROM " + messageTable +
				" WHERE id = ?";
			sql.prepared(getMessageQuery, new JsonArray().add(parentMessageId),
				SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
				public void handle(Either<String, JsonObject> event) {
					if(event.isLeft()){
						result.handle(event);
						return;
					}

					final JsonArray to = event.right().getValue().getArray("to");
					final JsonArray cc = event.right().getValue().getArray("cc");

					params.putArray("to", to)
						.putArray("cc", cc);

					String customReturn =
							"MATCH (v:Visible) " +
							"WHERE (v.id = visibles.id OR v.id IN {to} OR v.id IN {cc}) " +
							"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
							"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
							"visibles.profiles[0] as profile";

					findVisibles(eb, user.getUserId(), customReturn, params, true, true, true, acceptLanguage, preFilter, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray visibles) {
							JsonArray users = new JsonArray();
							JsonArray groups = new JsonArray();
							visible.putArray("groups", groups).putArray("users", users);
							for (Object o: visibles) {
								if (!(o instanceof JsonObject)) continue;
								JsonObject j = (JsonObject) o;
								if (j.getString("name") != null) {
									j.removeField("displayName");
									UserUtils.groupDisplayName(j, acceptLanguage);
									groups.add(j);
								} else {
									j.removeField("name");
									users.add(j);
								}
							}
							result.handle(new Either.Right<String,JsonObject>(visible));
						}
					});
				}
			}));
		} else {
			String customReturn =
					"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
					"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
					"visibles.profiles[0] as profile";
			findVisibles(eb, user.getUserId(), customReturn, params, true, true, true, acceptLanguage, preFilter, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibles) {
					JsonArray users = new JsonArray();
					JsonArray groups = new JsonArray();
					visible.putArray("groups", groups).putArray("users", users);
					for (Object o: visibles) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						if (j.getString("name") != null) {
							j.removeField("displayName");
							UserUtils.groupDisplayName(j, acceptLanguage);
							groups.add(j);
						} else {
							j.removeField("name");
							users.add(j);
						}
					}
					result.handle(new Either.Right<String,JsonObject>(visible));
				}
			});
		}
	}

	@Override
	public void createFolder(final String folderName, final String parentFolderId, final UserInfos user,
			final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folderName))
			return;

		final SqlStatementsBuilder builder = new SqlStatementsBuilder();
		final JsonObject messageObj = new JsonObject()
			.putString("id", UUID.randomUUID().toString())
			.putString("name", folderName)
			.putString("user_id", user.getUserId());

		if (parentFolderId != null) {
			JsonArray values = new JsonArray()
				.add(user.getUserId())
				.add(parentFolderId);
			String depthQuery = "SELECT depth FROM " + folderTable + " WHERE user_id = ? AND id = ?";
			sql.prepared(depthQuery, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
				public void handle(Either<String, JsonObject> event) {
					if(event.isLeft()){
						result.handle(event);
						return;
					}
					int parentDepth = event.right().getValue().getInteger("depth");
					if(parentDepth >= maxFolderDepth){
						result.handle(new Either.Left<String, JsonObject>("error.max.folder.depth"));
						return;
					}

					messageObj
						.putString("parent_id", parentFolderId)
						.putNumber("depth", parentDepth + 1);

					builder.insert(folderTable, messageObj);

					sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(result));
				}
			}));
		} else {
			sql.insert(folderTable, messageObj, SqlResult.validUniqueResultHandler(result));
		}

	}

	@Override
	public void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, data.getString("name")))
			return;

		String query = "UPDATE " + folderTable + " AS f " +
			"SET name = ? " +
			"WHERE f.id = ? AND f.user_id = ?";

		JsonArray values = new JsonArray()
			.add(data.getString("name"))
			.add(folderId)
			.add(user.getUserId());

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		String query =
			"SELECT f.* FROM " + folderTable + " AS f " +
			"WHERE f.user_id = ? AND f.trashed = false ";

		JsonArray values = new JsonArray()
			.add(user.getUserId());

		if(parentId == null){
			query += "AND f.parent_id IS NULL";
		} else {
			query += "AND f.parent_id = ?";
			values.add(parentId);
		}

		sql.prepared(query, values, SqlResult.validResultHandler(result));
	}

	@Override
	public void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		String query =
			"SELECT f.* FROM " + folderTable + " AS f " +
			"WHERE f.user_id = ? AND f.trashed = true ";

		JsonArray values = new JsonArray()
			.add(user.getUserId());

		sql.prepared(query, values, SqlResult.validResultHandler(result));
	}

	@Override
	public void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId))
			return;

		JsonArray values = new JsonArray();

		String query =
			"UPDATE " + userMessageTable + " AS um " +
			"SET folder_id = ? " +
			"WHERE um.user_id = ? AND um.message_id IN ";

		values
			.add(folderId)
			.add(user.getUserId());

		query += generateInVars(messageIds, values);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void backToSystemFolder(List<String> messageIds, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result))
			return;

		JsonArray values = new JsonArray();

		String query =
			"UPDATE " + userMessageTable + " AS um " +
			"SET folder_id = NULL " +
			"WHERE um.user_id = ? AND um.message_id IN ";

		values.add(user.getUserId());

		query += generateInVars(messageIds, values);
		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void trashFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String query =
			"UPDATE " + folderTable + " AS f " +
			"SET trashed = ? " +
			"WHERE f.id = ? AND f.user_id = ? AND f.trashed = ?";

		JsonArray values = new JsonArray()
			.add(true)
			.add(folderId)
			.add(user.getUserId())
			.add(false);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void restoreFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String query =
			"UPDATE " + folderTable + " AS f " +
			"SET trashed = ? " +
			"WHERE f.id = ? AND f.user_id = ? AND f.trashed = ?";

			JsonArray values = new JsonArray()
				.add(false)
				.add(folderId)
				.add(user.getUserId())
				.add(true);

			sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void deleteFolder(String folderId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result, folderId))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		/* Get all parent folders with recursion */

		String nonRecursiveTerm =
			"SELECT DISTINCT f.* FROM " + folderTable + " AS f " +
			"WHERE f.id = ? AND f.user_id = ? AND f.trashed = true ";
		JsonArray recursiveValues = new JsonArray().add(folderId).add(user.getUserId());

		String recursiveTerm =
			"SELECT f.* FROM " + folderTable + " AS f JOIN " +
			"parents ON f.parent_id = parents.id " +
			"WHERE f.user_id = ?";
		recursiveValues.add(user.getUserId());

		/* Get quota to free */

		String quotaRecursion =
			"WITH RECURSIVE parents AS ( "+
					nonRecursiveTerm +
					"UNION " +
					recursiveTerm +
			") " +
			"SELECT COALESCE(sum(um.total_quota), 0)::integer AS totalQuota FROM parents JOIN " +
			userMessageTable + " um ON um.folder_id = parents.id AND um.user_id = parents.user_id ";

		builder.prepared(quotaRecursion, recursiveValues);

		/* Physically delete the folder, which will start a cascading delete process for parent folders, messages and attachments. */

		String deleteFolder =
			"DELETE FROM " + folderTable + " f " +
			"WHERE f.id = ? AND f.user_id = ? AND f.trashed = true";
		JsonArray values = new JsonArray()
			.add(folderId)
			.add(user.getUserId());

		builder.prepared(deleteFolder, values);

		/* Perform the transaction */

		sql.transaction(builder.build(), SqlResult.validResultsHandler(result));

	}

	@Override
	public void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId))
			return;

		long attachmentSize = uploaded.getObject("metadata", new JsonObject()).getLong("size", 0);

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonObject attParams = new JsonObject()
			.putString("id", uploaded.getString("_id"))
			.putString("name", uploaded.getObject("metadata").getString("name"))
			.putString("filename", uploaded.getObject("metadata").getString("filename"))
			.putString("contentType", uploaded.getObject("metadata").getString("content-type"))
			.putString("contentTransferEncoding", uploaded.getObject("metadata").getString("content-transfer-encoding"))
			.putString("charset", uploaded.getObject("metadata").getString("charset"))
			.putNumber("size", attachmentSize);

		builder.insert(attachmentTable, attParams, "id");

		JsonObject umaParams = new JsonObject()
			.putString("user_id", user.getUserId())
			.putString("message_id", messageId)
			.putString("attachment_id", uploaded.getString("_id"));

		builder.insert(userMessageAttachmentTable, umaParams);

		String query =
			"UPDATE " + userMessageTable + " AS um " +
			"SET total_quota = total_quota + ? " +
			"WHERE um.user_id = ? AND um.message_id = ?";
		JsonArray values = new JsonArray()
			.add(attachmentSize)
			.add(user.getUserId())
			.add(messageId);

		builder.prepared(query, values);

		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId))
			return;

		String query =
			"SELECT att.* FROM " + attachmentTable + " att JOIN " +
			userMessageAttachmentTable + " uma ON uma.attachment_id = att.id " +
			"WHERE att.id = ? AND uma.user_id = ? AND uma.message_id = ?";

		JsonArray values = new JsonArray()
			.add(attachmentId)
			.add(user.getUserId())
			.add(messageId);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonArray values = new JsonArray()
			.add(messageId)
			.add(user.getUserId())
			.add(attachmentId);

		String query1 =
			"SELECT att.* FROM " + attachmentTable + " att WHERE att.id = ?";
		builder.prepared(query1, new JsonArray().add(attachmentId));

		String query2 =
			"SELECT (count(*) = 1) AS deletionCheck FROM " + attachmentTable + " att JOIN " +
			userMessageAttachmentTable + " uma ON uma.attachment_id = att.id " +
			"WHERE att.id = ? " +
			"GROUP BY att.id HAVING count(distinct uma.user_id) = 1 AND count(distinct uma.message_id) = 1";
		builder.prepared(query2, new JsonArray().add(attachmentId));

		String query3 =
			"WITH attachment AS (" +
				query1 +
			") " +
			"UPDATE " + userMessageTable + " AS um " +
			"SET total_quota = um.total_quota - (SELECT SUM(DISTINCT attachment.size) FROM attachment) " +
			"WHERE um.message_id = ? AND um.user_id = ?";
		JsonArray values3 = new JsonArray()
				.add(attachmentId)
				.add(messageId)
				.add(user.getUserId());
		builder.prepared(query3, values3);

		String query4 =
			"DELETE FROM " + userMessageAttachmentTable + " WHERE " +
			"message_id = ? AND user_id = ? AND attachment_id = ?";
		builder.prepared(query4, values);

		sql.transaction(builder.build(), SqlResult.validResultsHandler(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					result.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}
				else {
					JsonArray results = event.right().getValue();
					JsonObject attachment = (JsonObject) ((JsonArray) results.get(0)).get(0);
					boolean deletionCheck = ((JsonArray) results.get(1)).size() > 0 ?
							((JsonObject) ((JsonArray) results.get(1)).get(0)).getBoolean("deletioncheck", false) :
							false;
					JsonObject resultJson = new JsonObject()
						.putBoolean("deletionCheck", deletionCheck)
						.putString("fileId", attachment.getString("id"))
						.putNumber("fileSize", attachment.getLong("size"));

					result.handle(new Either.Right<String, JsonObject>(resultJson));
				}
			}
		}));
	}

	@Override
	public void forwardAttachments(String forwardId, String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId))
			return;

		String query =
			"WITH messageAttachments AS (" +
				"SELECT * FROM " + userMessageAttachmentTable + " " +
				"WHERE user_id = ? AND message_id = ?" +
			") " +
			"INSERT INTO " + userMessageAttachmentTable + " " +
			"SELECT user_id, ? AS message_id, attachment_id FROM messageAttachments";

		JsonArray values = new JsonArray()
				.add(user.getUserId())
				.add(forwardId)
				.add(messageId);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	///////////
	/* Utils */

	private String formatArray(JsonArray array){
		return formatArray(array, ",", "{", "}");
	}
	private String formatArray(JsonArray array, String delimiter, String prefix, String postfix){
		if(array.size() == 0){
			return "{}";
		}
		StringBuilder builder = new StringBuilder(prefix);
		for(Object obj : array){
			builder.append(obj.toString() + delimiter);
		}
		if(array.size() > 0)
			builder.delete(0, builder.length() - delimiter.length());
		builder.append(postfix);
		return builder.toString();
	}

	private String generateInVars(List<String> list, JsonArray values){
		StringBuilder builder = new StringBuilder();
		builder.append("(");

		for(String item : list){
			builder.append("?,");
			values.add(item);
		}
		if(list.size() > 0)
			builder.deleteCharAt(builder.length() - 1);
		builder.append(")");

		return builder.toString();
	}

	private String addFolderCondition(String folder, JsonArray values, String userId){
		String additionalWhere = "";
		switch(folder.toUpperCase()){
			case "INBOX":
				additionalWhere = "AND (m.from <> ? OR \"to\" @> ?::jsonb OR \"cc\" @> ?::jsonb) AND m.state = ? AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.add(userId);
				values.add(new JsonArray().add(userId).toString());
				values.add(new JsonArray().add(userId).toString());
				values.add("SENT");
				break;
			case "OUTBOX":
				additionalWhere = "AND m.from = ? AND m.state = ? AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.add(userId);
				values.add("SENT");
				break;
			case "DRAFT":
				additionalWhere = "AND m.from = ? AND m.state = ? AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.add(userId);
				values.add("DRAFT");
				break;
			case "TRASH":
				additionalWhere = "AND um.trashed = true";
				break;
		}
		return additionalWhere;
	}

	private boolean validationError(UserInfos user, Handler<Either<String, JsonArray>> results, String ... params) {
		if (user == null) {
			results.handle(new Either.Left<String, JsonArray>("conversation.invalid.user"));
			return true;
		}
		if (params.length > 0) {
			for (String s : params) {
				if (s == null) {
					results.handle(new Either.Left<String, JsonArray>("conversation.invalid.parameter"));
					return true;
				}
			}
		}
		return false;
	}

	private boolean validationParamsError(UserInfos user,
			Handler<Either<String, JsonObject>> result, String ... params) {
		if (user == null) {
			result.handle(new Either.Left<String, JsonObject>("conversation.invalid.user"));
			return true;
		}
		if (params.length > 0) {
			for (String s : params) {
				if (s == null) {
					result.handle(new Either.Left<String, JsonObject>("conversation.invalid.parameter"));
					return true;
				}
			}
		}
		return false;
	}

	private boolean validationError(UserInfos user, JsonObject c,
			Handler<Either<String, JsonObject>> result, String ... params) {
		if (c == null) {
			result.handle(new Either.Left<String, JsonObject>("conversation.invalid.fields"));
			return true;
		}
		return validationParamsError(user, result, params);
	}

}
