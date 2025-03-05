/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.conversation.service.impl;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isNotEmpty;

import static org.entcore.common.user.UserUtils.findVisibles;

import java.util.*;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerFormat;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.eventbus.DeliveryOptions;

import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.conversation.LegacySearchVisibleRequest;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.util.FolderUtil;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.Either.Right;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.conversation.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlConversationService implements ConversationService{
	public static final int DEFAULT_SENDTIMEOUT = 15 * 60 * 1000;
	private static final Logger log = LoggerFactory.getLogger(SqlConversationService.class);
	private final EventBus eb;
	private final Sql sql;

	private final int maxFolderDepth;

	private final String messageTable;
	private final String folderTable;
	private final String attachmentTable;
	private final String userMessageTable;
	private final String userMessageAttachmentTable;
	private final String originalMessageTable;
	private final boolean optimizedThreadList;
	private int sendTimeout = DEFAULT_SENDTIMEOUT;

	private final IContentTransformerClient contentTransformerClient;
	private final IContentTransformerEventRecorder contentTransformerEventRecorder;
	private final Set<String> CONVERSATION_TRANSFORMATION_EXTENSIONS = Collections.singleton("conversation-history");

	public SqlConversationService(Vertx vertx, String schema, IContentTransformerClient contentTransformerClient, IContentTransformerEventRecorder contentTransformerEventRecorder) {
		this.eb = Server.getEventBus(vertx);
		this.sql = Sql.getInstance();
		this.maxFolderDepth = Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH);
		messageTable = schema + ".messages";
		folderTable = schema + ".folders";
		attachmentTable = schema + ".attachments";
		userMessageTable = schema + ".usermessages";
		userMessageAttachmentTable = schema + ".usermessagesattachments";
		originalMessageTable = schema + ".originalmessages";
		optimizedThreadList = vertx.getOrCreateContext().config().getBoolean("optimized-thread-list", false);
		this.contentTransformerClient = contentTransformerClient;
		this.contentTransformerEventRecorder = contentTransformerEventRecorder;
		eb.consumer("conversation.legacy.search.visible", message -> {
			final JsonObject payload = (JsonObject) message.body();
			final LegacySearchVisibleRequest request = payload.mapTo(LegacySearchVisibleRequest.class);
			this.doFindVisibleRecipients(request.getParentMessageId(), request.getUserId(),
				request.getLanguage(), request.getSearch())
				.onSuccess(message::reply)
				.onFailure(th -> {
					log.warn("An error occurred while finding visibles", th);
					message.fail(500, th.getMessage());
				});
		});
	}

	public SqlConversationService setSendTimeout(int sendTimeout) {
		this.sendTimeout = sendTimeout;
		return this;
	}

	@Override
	public void saveDraft(String parentMessageId, String threadId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result, HttpServerRequest request) {
		save(parentMessageId, threadId, message, user, result, request);
	}

	private void save(String parentMessageId, String threadId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result, HttpServerRequest request){
		message
			.put("id", UUID.randomUUID().toString())
			.put("from", user.getUserId())
			.put("date", System.currentTimeMillis())
			.put("state", State.DRAFT.name());

		JsonObject m = Utils.validAndGet(message, MESSAGE_FIELDS, DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		if(parentMessageId != null)
			message.put("parent_id", parentMessageId);

		if(threadId != null){
			message.put("thread_id", threadId);
		}else{
			message.put("thread_id", message.getString("id"));
		}

		updateMessageWithTransformedContent(message, request).onSuccess(event -> {
			// 1 - Insert message
			builder.insert(messageTable, message, "id");

			// 2 - Link message to the user
			builder.insert(userMessageTable, new JsonObject()
					.put("user_id", user.getUserId())
					.put("message_id", message.getString("id")));

			sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
		}).onFailure(th -> {
			String contentTransformationError = "Content transformation failed for message with id : " + message.getString("id");
			log.error(contentTransformationError, th);
			result.handle(new Either.Left<>(contentTransformationError));
		});
	}

	@Override
	public void updateDraft(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result, HttpServerRequest request) {
		update(messageId, message, user, result, request);
	}

	private void update(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result, HttpServerRequest request) {
		message.put("date", System.currentTimeMillis())
				.put("from", user.getUserId());
		JsonObject m = Utils.validAndGet(message, UPDATE_DRAFT_FIELDS, UPDATE_DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result, messageId))
			return;

		updateMessageWithTransformedContent(message, request).onSuccess(event -> {
			StringBuilder sb = new StringBuilder();
			JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

			for (String attr : message.fieldNames()) {
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
		}).onFailure(th -> {
			String contentTransformationError = "Content transformation failed for message with id : " + message.getString("id");
			log.error(contentTransformationError, th);
			result.handle(new Either.Left<>(contentTransformationError));
		});
	}

	/**
	 * Update a message content with its transformed version
	 * @param message the message whose content must be transformed and updated
	 * @param request the request
	 * @return a future completed if the message has been updated with transformed content successfully
	 */
	private Future<Void> updateMessageWithTransformedContent(JsonObject message, HttpServerRequest request) {
		Promise<Void> updatedMessagePromise = Promise.promise();
		Future<ContentTransformerResponse> contentTransformerResponseFuture ;
		if (StringUtils.isEmpty(message.getString("body"))) {
			// no content to transform
			contentTransformerResponseFuture = Future.succeededFuture();
		} else {
			contentTransformerResponseFuture = transformMessageContent(message.getString("body"), message.getString("id"), request);
		}
		contentTransformerResponseFuture.onSuccess(transformerResponse -> {
			if (transformerResponse == null) {
				log.debug("No content transformed");
			} else {
				message.put("body", transformerResponse.getCleanHtml());
				message.put("content_version", transformerResponse.getContentVersion());
			}
			updatedMessagePromise.complete();
		}).onFailure(updatedMessagePromise::fail);
		return updatedMessagePromise.future();
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

		sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(senderId).add(messageId), SqlResult.validUniqueResultHandler(handler, "attachmentids"));
	}

	@Override
	public void send(final String parentMessageId, final String draftId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		final String getThreadId = "SELECT thread_id FROM conversation.messages WHERE id = ? ";
		sql.prepared(getThreadId, new JsonArray().add(draftId), SqlResult.validUniqueResultHandler(either -> {
			if (either.isRight()) {
				sendMessage(parentMessageId, draftId, either.right().getValue().getString("thread_id"), message, user, result);
			} else {
				sendMessage(parentMessageId, draftId, null, message, user, result);
			}
		}));
	}

	private void sendMessage(final String parentMessageId, final String draftId, final String threadId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, draftId))
			return;

		getSenderAttachments(user.getUserId(), draftId, new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					result.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}

				JsonArray attachmentIds = event.right().getValue().getJsonArray("attachmentids");
				long totalQuota = event.right().getValue().getLong("totalquota");
				String unread = "false";
				final JsonArray ids = message.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray());
				if(ids.contains(user.getUserId()))
					unread = "true";
				SqlStatementsBuilder builder = new SqlStatementsBuilder();

				String updateMessage =
						"UPDATE " + messageTable + " SET state = ? WHERE id = ? "+
								"RETURNING id, subject, body, thread_id";
				String updateUnread = "UPDATE " + userMessageTable + " " +
						"SET unread = " + unread +
						" WHERE user_id = ? AND message_id = ? ";
				builder.prepared(updateMessage, new fr.wseduc.webutils.collections.JsonArray().add("SENT").add(draftId));
				builder.prepared(updateUnread, new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).add(draftId));

				final String insertThread =
						"INSERT INTO conversation.threads as t (" +
						"SELECT thread_id as id, date, subject, \"from\", \"to\", cc, cci, \"displayNames\" " +
						"FROM conversation.messages m " +
						"WHERE m.id = ?) " +
						"ON CONFLICT (id) DO UPDATE SET date = EXCLUDED.date, subject = EXCLUDED.subject, \"from\" = EXCLUDED.\"from\", " +
						"\"to\" = EXCLUDED.\"to\", cc = EXCLUDED.cc, cci = EXCLUDED.cci, \"displayNames\" = EXCLUDED.\"displayNames\" " +
						"WHERE t.id = EXCLUDED.id ";
				builder.prepared(insertThread, new fr.wseduc.webutils.collections.JsonArray().add(draftId));

				final String insertUserThread =
						"INSERT INTO conversation.userthreads as ut (user_id,thread_id,nb_unread) VALUES (?,?,?) " +
						"ON CONFLICT (user_id,thread_id) DO UPDATE SET nb_unread = ut.nb_unread + 1 " +
						"WHERE ut.user_id = EXCLUDED.user_id AND ut.thread_id = EXCLUDED.thread_id";
				if (threadId != null) {
					builder.prepared(insertUserThread, new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).add(threadId).add(0));
				}

				for(Object toObj : ids){
					if(toObj.equals(user.getUserId()))
						continue;

					builder.insert(userMessageTable, new JsonObject()
						.put("user_id", toObj.toString())
						.put("message_id", draftId)
						.put("total_quota", totalQuota)
					);

					if (threadId != null) {
						builder.prepared(insertUserThread, new fr.wseduc.webutils.collections.JsonArray().add(toObj.toString()).add(threadId).add(1));
					}

					for(Object attachmentId : attachmentIds){
						builder.insert(userMessageAttachmentTable, new JsonObject()
							.put("user_id", toObj.toString())
							.put("message_id", draftId)
							.put("attachment_id", attachmentId.toString())
						);
					}
				}

				sql.transaction(builder.build(),new DeliveryOptions().setSendTimeout(sendTimeout), SqlResult.validUniqueResultHandler(0, result));
			}
		});
	}

	@Override
	public void list(String folder, Boolean unread, UserInfos user, int page, int pageSize, final String searchText, Handler<Either<String, JsonArray>> results) {
		list(folder,
			ConversationService.isSystemFolder(folder) ? null : "", // `restrain` can only applies to user's folders.
			unread, user, page, pageSize, searchText, results
		);
	}

	@Override
	public void list(String folder, String restrain, Boolean unread, UserInfos user, int page, final String searchText, Handler<Either<String, JsonArray>> results) {
		list(folder, restrain, unread, user, page, LIST_LIMIT, searchText, results);
	}

	protected void list(String folder, String restrain, Boolean unread, UserInfos user, int page, int pageSize, final String searchText, Handler<Either<String, JsonArray>> results)
	{
		if(page < 0)
		{
			results.handle(new Either.Right<String, JsonArray>(new JsonArray()));
			return;
		}
		if(LIST_LIMIT<pageSize || pageSize<1) pageSize = LIST_LIMIT;
		int skip = page * pageSize;

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		String messageConditionUnread = addMessageConditionUnread(folder, values, unread, user);
		String messagesFields = "m.id, m.subject, m.from, m.state, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\", m.cci, m.\"cciName\", m.\"displayNames\", m.date ";

		values.add("SENT").add(user.getUserId());
		String additionalWhere = addCompleteFolderCondition(values, restrain, unread, folder, user);

		if(searchText != null){
			additionalWhere += " AND m.text_searchable  @@ to_tsquery(m.language::regconfig, unaccent(?)) ";
			values.add(StringUtils.join(checkAndComposeWordFromSearchText(searchText), " & "));
		}
		String query = "SELECT "+messagesFields+", um.unread as unread, " +
				"CASE when COUNT(distinct r) = 0 THEN false ELSE true END AS response, COUNT(*) OVER() as count, " +
				"CASE when COUNT(distinct uma) = 0 THEN false ELSE true END AS  \"hasAttachment\" " +
				"FROM " + userMessageTable + " um LEFT JOIN " +
				userMessageAttachmentTable + " uma ON um.user_id = uma.user_id AND um.message_id = uma.message_id JOIN " +
				messageTable + " m ON (um.message_id = m.id" + messageConditionUnread + ") LEFT JOIN " +
				messageTable + " r ON um.message_id = r.parent_id AND r.from = um.user_id AND r.state= ? " +
				"WHERE um.user_id = ? " + additionalWhere + " " +
				"GROUP BY m.id, unread " +
				"ORDER BY m.date DESC LIMIT " + pageSize + " OFFSET " + skip;

		sql.prepared(query, values, SqlResult.validResultHandler(results, "attachments", "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames"));
	}

	//TODO : add to utils (similar function in SearchEngineController)
	private List<String> checkAndComposeWordFromSearchText(final String searchText) {
		List<String> searchWords = new ArrayList<>();
		final String searchTextTreaty = searchText.replaceAll("\\s+", " ").trim();
		if (!searchTextTreaty.isEmpty()) {
			String[] words = searchTextTreaty.split(" ");
			String tmp;
			for (String w : words) {
				tmp = w.replaceAll("(?!')\\p{Punct}", "");
				if(tmp.length() > 0)
					searchWords.add(tmp);
			}
		}
		return  searchWords;
	}

	/**
	 * Method listing the messages of a folder and formatting message summary data
	 * @param folderId the id
	 * @param unread whether a message has been read or not
	 * @param userInfos the user infos
	 * @param page the number of the page to display
	 * @param pageSize the number of element to display in the page
	 * @param search a text search filter
	 * @param lang the user language
	 * @return a future of an array containing the folder's messages summary data to display
	 */
	@Override
	public Future<JsonArray> listAndFormat(String folderId, Boolean unread, UserInfos userInfos, int page, int pageSize, String search, String lang) {
		final Promise<JsonArray> promise = Promise.promise();
		final JsonObject userIndex = new JsonObject();
		final JsonObject groupIndex = new JsonObject();
		this.list(folderId, unread, userInfos, page, pageSize, search, either -> {
			if (either.isRight()) {
				final JsonArray messages = either.right().getValue();
				for (Object message : messages) {
					if (!(message instanceof JsonObject)) {
						continue;
					}
					// Extract distinct users and groups.
					MessageUtil.computeUsersAndGroupsDisplayNames((JsonObject) message, userInfos, lang, userIndex, groupIndex);
				}

				MessageUtil.loadUsersAndGroupsDetails(eb, userInfos, userIndex, groupIndex)
						.onSuccess( unused -> {
							for (Object m : messages) {
								if (!(m instanceof JsonObject)) {
									continue;
								}
								MessageUtil.formatRecipients((JsonObject) m, userIndex, groupIndex);
							}
							promise.complete(messages);
						})
						.onFailure( throwable -> {
							promise.fail(throwable.getMessage());
						});
			} else {
				promise.fail(either.left().getValue());
			}
		});
		return promise.future();
	}

	@Override
	public void listThreads(UserInfos user, int page, Handler<Either<String, JsonArray>> results) {
		int nbThread =  10;
		int skip = page * nbThread;
		String messagesFields = "id, date, subject, \"displayNames\", \"to\", \"from\", cc, cci ";
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		values.add(user.getUserId());
		final String query;
		if (optimizedThreadList) {
			query =
				"SELECT t.id as id, t.date as date, t.subject as subject, t.\"displayNames\" as \"displayNames\", " +
				"t.\"to\" as \"to\", t.\"from\" as \"from\", t.cc as cc, t.cci as cci, ut.nb_unread as unread " +
				"FROM conversation.userthreads ut " +
				"LEFT JOIN conversation.threads t on ut.thread_id = t.id " +
				"WHERE  ut.user_id = ? " +
				"ORDER BY date DESC " +
				"LIMIT " + nbThread + " OFFSET " + skip;
		} else {
			query =
				" WITH threads AS ( " +
				" SELECT * from (SELECT  DISTINCT ON (m.thread_id) thread_id AS "+messagesFields+ " FROM " + userMessageTable + " um " +
				" JOIN "+messageTable+" m ON um.message_id = m.id " +
				" WHERE um.user_id = ? AND m.state = 'SENT' AND um.trashed = false ORDER BY m.thread_id, m.date DESC) a "+
				" ORDER BY date DESC LIMIT "+ nbThread +" OFFSET "+ skip + ") " +

				"SELECT "+ messagesFields +", unread FROM threads JOIN (SELECT m.thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) AS unread " +
				"FROM threads, conversation.usermessages um JOIN conversation.messages m ON um.message_id = m.id and um.user_id= ? " +
				"WHERE  um.trashed = false AND m.thread_id=threads.id GROUP BY m.thread_id) c ON threads.id = c.thread_id " +
				"ORDER BY date DESC";
			values.add(user.getUserId());
		}
		sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "cci", "ccName", "displayNames"));
	}

	@Override
	public void listThreadMessages(String threadId, int page, UserInfos user, Handler<Either<String, JsonArray>> results) {
		int skip = page * LIST_LIMIT;
		String messagesFields = "m.id, m.parent_id, m.subject, m.body, m.from, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\",  m.cci, m.\"cciName\", m.\"displayNames\", m.date, m.thread_id ";
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

		values.add(user.getUserId());
		values.add(threadId);

		String query =
				"SELECT "+messagesFields+", um.unread as unread, " +
				" CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
				" FROM " +userMessageTable + " as um" +
				" JOIN "+messageTable+" as m ON um.message_id = m.id " +
				" LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
				" LEFT JOIN " + attachmentTable + " att " +
				" ON att.id = uma.attachment_id " +
				" WHERE um.user_id = ? AND m.thread_id = ? " +
				" AND m.state = 'SENT' AND um.trashed = false " +
				" GROUP BY m.id, um.unread " +
				" ORDER BY m.date DESC LIMIT " + LIST_LIMIT + " OFFSET " + skip;

		sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames", "attachments"));
	}

	@Override
	public void listThreadMessagesNavigation(String messageId, boolean previous, UserInfos user, Handler<Either<String, JsonArray>> results) {
		int maxMessageInThread = 15;
		String messagesFields = "m.id, m.parent_id, m.subject, m.body, m.from, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\", m.cci, m.\"cciName\", m.\"displayNames\", m.date, m.thread_id ";
		String condition, limit;
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

		if(previous){
			condition = " m.date < element.date ";
			limit = " LIMIT "+ maxMessageInThread +" OFFSET 0";
		}else{
			condition = " m.date > element.date ";
			limit = "";
		}
		values.add(messageId);
		values.add(user.getUserId());

		String query = "WITH element AS ( " +
				" SELECT thread_id, date FROM "+messageTable+" WHERE id = ? ) " +
				" SELECT "+messagesFields+", um.unread as unread, " +
				" CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
				" FROM element, " +userMessageTable + " as um " +
				" JOIN "+messageTable+" as m ON um.message_id = m.id " +
				" LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
				" LEFT JOIN " + attachmentTable + " att " +
				" ON att.id = uma.attachment_id " +
				" WHERE um.user_id = ? AND m.thread_id = element.thread_id " +
				" AND " + condition +
				" AND m.state = 'SENT' AND um.trashed = false " +
				" GROUP BY m.id, um.unread " +
				" ORDER BY m.date DESC" + limit;

		sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames", "attachments"));
	}


	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

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

		final String deleteUserThreads =
			"DELETE FROM conversation.userthreads " +
			"WHERE user_id = ? AND thread_id NOT IN (" +
				"SELECT DISTINCT m.thread_id " +
				"FROM conversation.usermessages um " +
				"LEFT JOIN conversation.messages m on um.message_id = m.id " +
				"WHERE user_id = ? AND trashed = false " +
			")";
		final JsonArray values2 = new JsonArray().add(user.getUserId()).add(user.getUserId());

		SqlStatementsBuilder builder = new SqlStatementsBuilder();
		builder.prepared(query.toString(), values);
		builder.prepared(deleteUserThreads, values2);
		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void trashThread(List<String> threadIds, UserInfos user, Handler<Either<String, JsonObject>> result){
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		StringBuilder query = new StringBuilder(
				"UPDATE " + userMessageTable + " AS um  " +
					"SET trashed = true " +
					"FROM conversation.messages as m " +
					"WHERE m.thread_id IN ");

		query.append(generateInVars(threadIds, values));
		query.append(" AND um.user_id = ? AND um.trashed = false AND um.message_id = m.id ");
		values.add(user.getUserId());

		final String deleteUserThreads =
			"DELETE FROM conversation.userthreads " +
			"WHERE thread_id IN " + Sql.listPrepared(threadIds.toArray()) + " AND user_id = ? ";
		final JsonArray values2 = new JsonArray(threadIds).add(user.getUserId());

		SqlStatementsBuilder builder = new SqlStatementsBuilder();
		builder.prepared(query.toString(), values);
		builder.prepared(deleteUserThreads, values2);
		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));

	}


	@Override
	public void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result)) return;

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " " +
			"SET trashed = false " +
			"WHERE trashed = true AND user_id = ? AND message_id IN ");

		values.add(user.getUserId());

		query.append(generateInVars(messagesId, values));

		final String insertUserThread =
				"INSERT INTO conversation.userthreads ( " +
				"SELECT um.user_id as user_id, m.thread_id as thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) as unread " +
				"FROM conversation.usermessages um " +
				"JOIN conversation.messages m on um.message_id = m.id " +
				"WHERE um.message_id IN " + Sql.listPrepared(messagesId.toArray()) + " AND um.user_id = ? AND um.trashed = false AND m.state = 'SENT' " +
				"GROUP BY user_id, m.thread_id) ON CONFLICT (user_id,thread_id) DO NOTHING";
		JsonArray values2 = new JsonArray(messagesId).add(user.getUserId());

		SqlStatementsBuilder builder = new SqlStatementsBuilder();
		builder.prepared(query.toString(), values);
		builder.prepared(insertUserThread, values2);
		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void delete(List<String> messagesId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonArray values2 = new fr.wseduc.webutils.collections.JsonArray();
		JsonArray values3 = new fr.wseduc.webutils.collections.JsonArray();
		values2.add(user.getUserId());
		values3.add(user.getUserId());

		String getTotalQuota =
			"SELECT coalesce(sum(um.total_quota), 0)::integer AS totalQuota FROM " + userMessageTable + " um " +
			"WHERE um.user_id = ? AND um.trashed = true";

		String deleteUserMessages =
			"DELETE FROM " + userMessageTable + " um " +
			"WHERE um.user_id = ? AND um.trashed = true";

		if (!deleteAll) {
			getTotalQuota += " AND um.message_id IN ";
			getTotalQuota += (generateInVars(messagesId, values2));
			deleteUserMessages += " AND um.message_id IN ";
			deleteUserMessages += (generateInVars(messagesId, values3));
		}

		builder.prepared(getTotalQuota, values2);
		builder.prepared(deleteUserMessages, values3);

		sql.transaction(builder.build(), SqlResult.validResultsHandler(result));
	}

	@Override
	public void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		get(messageId, user, 0, result);
	}

	@Override
	public void get(String messageId, UserInfos user, int apiVersion, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		String decrUnreadThread =
				"UPDATE conversation.userThreads " +
				"SET nb_unread = nb_unread - 1 " +
				"WHERE user_id = ? AND thread_id = (" +
					"SELECT m.thread_id " +
					"FROM conversation.usermessages um " +
					"JOIN conversation.messages m on um.message_id = m.id " +
					"WHERE um.user_id = ? AND um.message_id = ? AND um.unread = true) " +
				"AND nb_unread > 0 ";

		String updateQuery = "UPDATE " + userMessageTable + " " +
			"SET unread = false " +
			"WHERE user_id = ? AND message_id = ? ";

		String selectQuery =
			"SELECT " +
				"m.*, " +
				(apiVersion>0 ? "um.folder_id as folder_id, um.trashed as trashed, um.unread as unread, " +
								"CASE WHEN count(distinct om.message_id) = 0 THEN false ELSE true END AS original_format_exists, " : "") +
				"CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
			"FROM " + messageTable + " m " +
			"JOIN " + userMessageTable + " um " +
				"ON m.id = um.message_id " +
			"LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
			"LEFT JOIN " + attachmentTable + " att " +
				"ON att.id = uma.attachment_id " +
			"LEFT JOIN " + originalMessageTable + " om " +
				"ON m.id = om.message_id " +
			"WHERE um.user_id = ? AND m.id = ?  " +
			(apiVersion>0 ? "GROUP BY m.id, um.folder_id, um.trashed, um.unread, om.message_id" : "GROUP BY m.id");

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(user.getUserId())
			.add(messageId);

		final JsonArray tValues = new fr.wseduc.webutils.collections.JsonArray()
			.add(user.getUserId())
			.add(user.getUserId())
			.add(messageId);

		builder.prepared(decrUnreadThread, tValues);
		builder.prepared(updateQuery, values);
		builder.prepared(selectQuery, values);

		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(2, result, "attachments", "to", "toName", "cc", "ccName", "displayNames", "cci", "cciName"));
	}

	/**
	 * Method fetching and formatting a message details
	 * @param id the id of the message to fetch and format
	 * @param userInfos the user infos
	 * @param lang the user language
	 * @param originalFormat true if the message body must be rendered with the original format, false by default
	 * @param request the request
	 * @return a {@link Future} of the message details to be rendered, after several formatting operations :
	 * <ul>
	 *     <li>transformation of message content</li>
	 *     <li>a series of operation to retrieve users and groups display names and details</li>
	 * </ul>
	 */
	@Override
	public Future<JsonObject> getAndFormat(String id, UserInfos userInfos, String lang, boolean originalFormat, HttpServerRequest request) {
		final Promise<JsonObject> promise = Promise.promise();
		final JsonObject userIndex = new JsonObject();
		final JsonObject groupIndex = new JsonObject();
		this.get(id, userInfos, 1, either -> {
			if (either.isRight()) {
				final JsonObject message = either.right().getValue();
				formatMessageContent(id, originalFormat, request, message)
						.onSuccess(event -> {
							// Extract distinct users and groups.
							MessageUtil.computeUsersAndGroupsDisplayNames(message, userInfos, lang, userIndex, groupIndex);

							MessageUtil.loadUsersAndGroupsDetails(eb, userInfos, userIndex, groupIndex)
									.onSuccess( unused -> {
										MessageUtil.formatRecipients(message, userIndex, groupIndex);
										promise.complete(message);
									})
									.onFailure( throwable -> {
										promise.fail(throwable.getMessage());
									});})
						.onFailure(th -> {
							promise.fail(th.getMessage());
						});
			} else {
				promise.fail(either.left().getValue());
			}
		});
		return promise.future();
	}

	/**
	 * Method formatting the message content according to the requested content version
	 * @param messageId the message id
	 * @param originalFormat true if original format of the message content must be returned
	 * @param request the request
	 * @param message the message details to return
	 * @return a void future completed if all message content transformations succeeded
	 */
	private Future<Void> formatMessageContent(String messageId, boolean originalFormat, HttpServerRequest request, JsonObject message) {
		Promise<Void> updatedMessagePromise= Promise.promise();
		// replace message body with original content if requested
		if (originalFormat) {
			Future<String> originalContentFuture = this.getOriginalMessageContent(messageId);
			originalContentFuture
					.onSuccess(originalContent -> {
						message.put("body", originalContent);
						updatedMessagePromise.complete();
					})
					.onFailure(throwable -> {
						log.error("Failed to retrieve original message content", throwable);
						updatedMessagePromise.fail(throwable);
					});
		}
		// transform and persist message content if needed
		else if (message.getInteger("content_version") == 0) {
			if (StringUtils.isEmpty(message.getString("body"))) {
				// no content to transform
				updateMessageContent(messageId, "", 1)
						.onSuccess(res -> {
							message.put("content_version", 1);
							updatedMessagePromise.complete();
						})
						.onFailure(throwable -> {
							log.error("Failed to update message with content version", throwable);
							updatedMessagePromise.fail(throwable);
						});
			} else {
				transformMessageContent(message.getString("body"), messageId, request)
						.onSuccess(transformerResponse -> updateMessageContent(messageId, transformerResponse.getCleanHtml(), transformerResponse.getContentVersion())
								.onSuccess(res -> {
									message.put("body", transformerResponse.getCleanHtml());
									message.put("content_version", transformerResponse.getContentVersion());
									updatedMessagePromise.complete();
								})
								.onFailure(throwable -> {
									log.error("Failed to update message with transformed content", throwable);
									updatedMessagePromise.fail(throwable);
								}))
						.onFailure(throwable -> {
							log.error("Failed to transform message content", throwable);
							updatedMessagePromise.fail(throwable);
						});
			}
		// message content has already been transformed
		} else {
			updatedMessagePromise.complete();
		}
		return updatedMessagePromise.future();
	}

	/**
	 * Retrieve the original content of a message (i.e. before being transformed) in the dedicated table : conversation.originalmessages
	 * @param messageId the id of the message whose original content must be retrieved
	 * @return a {@link Future} of the original content
	 */
	@Override
	public Future<String> getOriginalMessageContent(String messageId) {
		Promise<String> originalMessagePromise = Promise.promise();
		String query = "" +
				"SELECT body " +
				"FROM " + originalMessageTable + " om " +
				"WHERE om.message_id = ?";
		JsonArray values = new JsonArray().add(messageId);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(sqlResult -> {
			if (sqlResult.isLeft()) {
				originalMessagePromise.fail("Failed fetching message original content : " + sqlResult.left().getValue());
			} else {
				JsonObject result = sqlResult.right().getValue();
				if (result.getString("body") == null) {
					originalMessagePromise.fail("No original content found for message with id : " + messageId);
				} else {
					originalMessagePromise.complete(result.getString("body"));
				}
			}
		}));
		return originalMessagePromise.future();
	}

	/**
	 * Transform the content of a message
	 * @param originalMessageContent the content of the message to be transformed
	 * @param messageId the id of the message to transform
	 * @param request the request
	 * @return a {@link Future} of the {@link ContentTransformerResponse} (containing the transformed content, the content version...)
	 */
	@Override
	public Future<ContentTransformerResponse> transformMessageContent(String originalMessageContent, String messageId, HttpServerRequest request) {
		Promise<ContentTransformerResponse> transformedMessagePromise = Promise.promise();
		contentTransformerClient.transform(new ContentTransformerRequest(
				new HashSet<>(Arrays.asList(ContentTransformerFormat.HTML, ContentTransformerFormat.JSON)),
				0,
				originalMessageContent,
				null,
				CONVERSATION_TRANSFORMATION_EXTENSIONS)
				).onSuccess(transformerResponse -> {
					contentTransformerEventRecorder.recordTransformation(messageId, "message", transformerResponse, request);
					transformedMessagePromise.complete(transformerResponse);
				})
				.onFailure(throwable -> {
					log.error("Failed transforming message content", throwable);
					transformedMessagePromise.fail(throwable);
				});
		return transformedMessagePromise.future();
	}

	/**
	 * Update a message content (its body and content version)
	 * @param messageId the id of the message to udpate
	 * @param body the new message body
	 * @param contentVersion the new message content version
	 * @return a {@link Future} whether the query has been performed
	 */
	@Override
	public Future<Void> updateMessageContent(String messageId, String body, int contentVersion) {
		Promise<Void> updatedPromise = Promise.promise();
		String updateQuery = "" +
				"UPDATE " + messageTable + " m " +
				"SET body = ? , content_version = ? " +
				"WHERE m.id = ? ";
		JsonArray values = new JsonArray()
				.add(body)
				.add(contentVersion)
				.add(messageId);
		sql.prepared(updateQuery, values, SqlResult.validUniqueResultHandler(sqlResult -> {
			if (sqlResult.isLeft()) {
				updatedPromise.fail("Failed updating message body : " + sqlResult.left().getValue());
			} else {
				updatedPromise.complete();
			}
		}));
		return updatedPromise.future();
	}

	@Override
	public void count(String folder, String restrain, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folder))
			return;

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

		String messageConditionUnread = addMessageConditionUnread(folder, values, unread, user);
		values.add(user.getUserId());

		String query = "SELECT count(*) as count FROM " + userMessageTable + " um JOIN " +
			messageTable + " m ON (um.message_id = m.id" + messageConditionUnread + ") " +
			"WHERE user_id = ? ";

		query += addCompleteFolderCondition(values, restrain, unread, folder, user);

		if(restrain != null && unread){
			query += " AND (m.from <> ? OR m.to @> ?::jsonb OR m.cc @> ?::jsonb) ";
			values.add(user.getUserId());
			values.add(new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).toString());
			values.add(new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).toString());
		}

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}



	@Override
	public void findVisibleRecipients(final String parentMessageId, final UserInfos user,
			final String acceptLanguage, final String search, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) {
			return;
		}
		doFindVisibleRecipients(parentMessageId, user.getUserId(), acceptLanguage, search)
			.onSuccess(data -> result.handle(new Either.Right<>(data)))
			.onFailure(th -> result.handle(new Either.Left<>(th.getMessage())));
	}

	private Future<JsonObject> doFindVisibleRecipients(final String parentMessageId, final String userId,
																						  final String acceptLanguage, final String search) {
		final Promise<JsonObject> promise = Promise.promise();
		final JsonObject visible = new JsonObject();

		final JsonObject params = new JsonObject();

		final String preFilter;
		if (isNotEmpty(search)) {
			preFilter = "AND (m:Group OR m.displayNameSearchField CONTAINS {search}) ";
			params.put("search", StringValidation.sanitize(search));
		} else {
			preFilter = null;
		}

		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			String getMessageQuery = "SELECT m.* FROM " + messageTable +
				" WHERE id = ?";
			sql.prepared(getMessageQuery, new fr.wseduc.webutils.collections.JsonArray().add(parentMessageId),
				SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
				public void handle(Either<String, JsonObject> event) {
					if(event.isLeft()){
						promise.fail(event.left().getValue());
						return;
					}

					final JsonArray to = event.right().getValue().getJsonArray("to");
					final JsonArray cc = event.right().getValue().getJsonArray("cc");

					params.put("to", to)
						.put("cc", cc);

					String customReturn =
							"MATCH (v:Visible) " +
							"WHERE (v.id = visibles.id OR v.id IN {to} OR v.id IN {cc}) " +
							"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
							"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
							"visibles.profiles[0] as profile, visibles.structureName as structureName, visibles.filter as groupProfile ";
					callFindVisibles(userId, acceptLanguage, visible, params, preFilter, customReturn).onComplete(promise);
				}
			}));
		} else {
			String customReturn =
					"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
					"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
					"visibles.profiles[0] as profile, visibles.structureName as structureName, visibles.filter as groupProfile";
			callFindVisibles(userId, acceptLanguage, visible, params, preFilter, customReturn).onComplete(promise);
		}
		return promise.future();
	}

	private Future<JsonObject> callFindVisibles(final String userId, final String acceptLanguage,
			final JsonObject visible, JsonObject params, String preFilter, String customReturn) {
		final Promise<JsonObject> promise = Promise.promise();
		findVisibles(eb, userId, customReturn, params, true, true, false, acceptLanguage, preFilter, visibles -> {
      JsonArray users = new fr.wseduc.webutils.collections.JsonArray();
      JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
      visible.put("groups", groups).put("users", users);

      log.info("callFindVisibles Count = " + visibles.size());

      for (Object o: visibles) {
        if (!(o instanceof JsonObject)) continue;
        JsonObject j = (JsonObject) o;
        // NOTE: the management rule below is "if a visible JsonObject has a non-null *name* field, then it is a Group".
        // TODO It should be defined more clearly. See #39835

        if (j.getString("name") != null) {
          if( j.getString("groupProfile") == null ) {
            // This is a Manual group, without a clearly defined "profile" (neither Student nor Teacher nor...) => Set it as "Manual"
            j.put("groupProfile", "Manual");
          }
          j.remove("displayName");
          UserUtils.groupDisplayName(j, acceptLanguage);
          j.put("profile", j.remove("groupProfile"));	// JCBE: set the *profile* field for this Group.
          groups.add(j);
        } else {
          j.remove("name");
          j.remove("groupProfile");	// JCBE: remove this unused and empty data for a User.
          users.add(j);
        }
      }
      promise.complete(visible);
    });
		return promise.future();
	}

	@Override
	public void toggleUnread(List<String> messagesIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;
		final String getThreadIds = "SELECT thread_id FROM conversation.messages WHERE id IN " + Sql.listPrepared(messagesIds.toArray());
		sql.prepared(getThreadIds, new JsonArray(messagesIds), SqlResult.validResultHandler(either -> {
			if (either.isRight()) {
				SqlStatementsBuilder builder = new SqlStatementsBuilder();
				JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
				String query = "UPDATE " + userMessageTable + " " +
						"SET unread = ? " +
						"WHERE user_id = ? AND message_id IN "  + Sql.listPrepared(messagesIds.toArray());

				values.add(unread);
				values.add(user.getUserId());
				for(String id : messagesIds){
					values.add(id);
				}

				builder.prepared(query, values);

				final List<String> threadIds = new ArrayList<>();
				for (Object row: either.right().getValue()) {
					if (!(row instanceof JsonObject)) continue;
					threadIds.add(((JsonObject) row).getString("thread_id"));
				}
				recalculateNbUnreadInThreads(threadIds, user, builder);

				sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
			} else {
				result.handle(new Either.Left<>(either.left().getValue()));
			}
		}));
	}

	@Override
	public void toggleUnreadThread(List<String> threadIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		SqlStatementsBuilder builder = new SqlStatementsBuilder();
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		StringBuilder query = new StringBuilder(
				"UPDATE " + userMessageTable + " AS um  " +
						"SET  unread = ? " +
						"FROM conversation.messages as m " +
						"WHERE m.thread_id IN ");
		values.add(unread);
		query.append(generateInVars(threadIds, values));
		query.append(" AND um.user_id = ? AND um.message_id = m.id ");
		values.add(user.getUserId());

		builder.prepared(query.toString(), values);
		recalculateNbUnreadInThreads(threadIds, user, builder);
		sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	private void recalculateNbUnreadInThreads(List<String> threadIds, UserInfos user, SqlStatementsBuilder builder) {
		final String query =
				"UPDATE conversation.userthreads as ut " +
				"SET nb_unread = g.unread " +
				"FROM (" +
					"SELECT um.user_id as user_id, m.thread_id as thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) as unread " +
					"FROM conversation.usermessages um " +
					"JOIN conversation.messages m on um.message_id = m.id " +
					"WHERE m.thread_id IN " + Sql.listPrepared(threadIds.toArray()) + " AND um.user_id = ? AND m.state = 'SENT' " +
					"GROUP BY user_id, m.thread_id " +
				") as g " +
				"WHERE ut.user_id = g.user_id AND ut.thread_id = g.thread_id ";

		builder.prepared(query, new JsonArray(threadIds).add(user.getUserId()));
	}
	private boolean isDuplicateError(String msg){
		return msg.contains("violates unique constraint") || msg.contains("rompt la contrainte unique");
	}


	@Override
	public void createFolder(final String folderName, final String parentFolderId, final UserInfos user,
			final Handler<Either<String, JsonObject>> resultOriginal) {
		if (validationParamsError(user, resultOriginal, folderName))
			return;
		final String id = UUID.randomUUID().toString();
		final Handler<Either<String, JsonObject>> result = res->{
			if(res.isLeft()){
				if(isDuplicateError(res.left().getValue())){
					resultOriginal.handle(new Either.Left<String,JsonObject>("conversation.error.duplicate.folder"));
				}else{
					resultOriginal.handle(res.left());
				}
			}else{
				Right<String,JsonObject> right = res.right();
				right.getValue().put("id", id);
				resultOriginal.handle(right);
			}
		};
		final SqlStatementsBuilder builder = new SqlStatementsBuilder();
		final JsonObject messageObj = new JsonObject()
			.put("id", id)
			.put("name", folderName)
			.put("user_id", user.getUserId());

		if (parentFolderId != null) {
			JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
				.add(user.getUserId())
				.add(parentFolderId);
			String depthQuery = "SELECT depth FROM " + folderTable + " WHERE user_id = ? AND id = ?";
			sql.prepared(depthQuery, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
				public void handle(Either<String, JsonObject> event) {
					if(event.isLeft()){
						resultOriginal.handle(event);
						return;
					}
					int parentDepth = event.right().getValue().getInteger("depth");
					if(parentDepth >= maxFolderDepth){
						resultOriginal.handle(new Either.Left<String, JsonObject>("error.max.folder.depth"));
						return;
					}

					messageObj
						.put("parent_id", parentFolderId)
						.put("depth", parentDepth + 1);

					builder.insert(folderTable, messageObj);

					sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(result));
				}
			}));
		} else {
			sql.insert(folderTable, messageObj, SqlResult.validUniqueResultHandler(result));
		}

	}

	@Override
	public void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> resultOriginal) {
		if (validationParamsError(user, resultOriginal, data.getString("name")))
			return;
		final Handler<Either<String, JsonObject>> result = res->{
			if(res.isLeft()){
				if(isDuplicateError(res.left().getValue())){
					resultOriginal.handle(new Either.Left<String,JsonObject>("conversation.error.duplicate.folder"));
				}else{
					resultOriginal.handle(res.left());
				}
			}else{
				resultOriginal.handle(res.right());
			}
		};
		String query = "UPDATE " + folderTable + " AS f " +
			"SET name = ?, skip_uniq=FALSE " +
			"WHERE f.id = ? AND f.user_id = ?";

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(data.getString("name"))
			.add(folderId)
			.add(user.getUserId());

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	/**
	 * @param user
	 * @param depth requested levels of results (when parentId is non-present)
	 * @param parentId When present, depth is limited to 1
	 * @param result
	 */
	@Override
	public void getFolderTree(final UserInfos user, int depth, final Optional<String> parentId, final Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result, (0<depth && depth<=MAX_FOLDERS_LEVEL) ? "ok":(String)null))
			return;

		final StringBuilder query = new StringBuilder()
		.append("WITH sub AS (")
		.append(" SELECT COUNT(um.message_id) as nb_messages, COALESCE(SUM(CASE WHEN um.unread = TRUE THEN 1 ELSE 0 END), 0) as nb_unread, um.folder_id")
		.append(" FROM ").append(userMessageTable).append(" um ")
		.append(" INNER JOIN ").append(messageTable).append(" m ON (um.message_id = m.id AND m.state='SENT')")
		.append(" WHERE um.folder_id IS NOT NULL AND um.trashed = FALSE AND um.user_id = ? ")
		.append(" GROUP BY um.folder_id")
		.append(") ")
		.append(" SELECT f.id, f.parent_id, f.name, f.depth")
		.append("   ,COALESCE(sub.nb_messages,0) as \"nbMessages\"")
		.append("   ,COALESCE(sub.nb_unread,0) as \"nbUnread\"")
		.append(" FROM ").append(folderTable).append(" AS f")
		.append(" LEFT JOIN sub ON (f.id=sub.folder_id)")
		.append(" WHERE f.user_id = ? AND f.trashed = FALSE")
		;
		final JsonArray values = new JsonArray()
		.add(user.getUserId())
		.add(user.getUserId());

		// Apply parentId / depth filters
		if(parentId.isPresent()) {
			// Limit depth to subfolders of this parent folder.
			query.append(" AND f.parent_id = ?");
			values.add(parentId.get());
			depth = 1;
		} else {
			query.append(" AND ? <= f.depth AND f.depth <= ?");
			values.add(1).add(depth);
		}

		final int finalDepth = depth;

		// When depth is 1, the resulting list contains the tree leaves.
		sql.prepared(query.toString(), values, SqlResult.validResultHandler(finalDepth<2 ? result : either->{
			// More process is only needed when depth is greater than 1.
			if( either.isLeft() ) {
				result.handle(either);
				return;
			}
			final JsonArray tree = FolderUtil.listToTree(getOrElse(either.right().getValue(), new JsonArray()), finalDepth);
			result.handle(new Either.Right<>(tree));
		}));
	}

	/**
	 * Retrieve a folder.
	 * @param folderId
	 * @return folder data
	 */
	private Future<JsonObject> getFolder(final String folderId) {
		Promise<JsonObject> promise = Promise.promise();
		sql.prepared(
			"SELECT f.* FROM " + folderTable + " AS f WHERE f.id = ?",
			new JsonArray().add(folderId),
			SqlResult.validUniqueResultHandler(either -> {
				if( either.isLeft() ) {
					promise.fail(either.left().getValue());
				} else {
					promise.complete(either.right().getValue());
				}
			})
		);
		return promise.future();
	}

	@Override
	public void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		String query =
			"SELECT f.* FROM " + folderTable + " AS f " +
			"WHERE f.user_id = ? AND f.trashed = false ";

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
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
	public void listUserFolders(Optional<String>  parentId, UserInfos user, Boolean unread, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
		return;
		final JsonArray subValues = new JsonArray();
		final StringBuilder subQuery = new StringBuilder();
		subQuery.append("SELECT count(*) as count,um.folder_id  FROM ").append(userMessageTable).append(" um ");
		subQuery.append(" INNER JOIN ").append(messageTable).append(" m ON (um.message_id = m.id) ");
		subQuery.append(" WHERE um.user_id = ?  AND m.state='SENT' ");
		subQuery.append(" AND (m.from <> ? OR m.to @> ?::jsonb OR m.cc @> ?::jsonb) ");
		subValues.add(user.getUserId());
		subValues.add(user.getUserId());
		subValues.add(new JsonArray().add(user.getUserId()).toString());
		subValues.add(new JsonArray().add(user.getUserId()).toString());
		if(unread != null && unread){
			subQuery.append(" AND um.unread = ").append(unread ? " TRUE " : " FALSE ");
		}
		subQuery.append(" GROUP BY um.folder_id ");
		//values SENT
		final JsonArray values = subValues.copy();
		final StringBuilder query = new StringBuilder();
		query.append("SELECT f.*, COALESCE(sub.count,0) as \"nbUnread\" FROM ").append(folderTable).append(" AS f ");
		query.append("LEFT JOIN (").append(subQuery).append(") AS sub ON (f.id=sub.folder_id) ");
		query.append("WHERE f.user_id = ? AND f.trashed IS FALSE ");
		values.add(user.getUserId());
		if(parentId.isPresent()){
			query.append(" AND f.parent_id = ? ");
			values.add(parentId.get());
		}else{
			query.append(" AND f.parent_id IS NULL ");
		}
		sql.prepared(query.toString(), values, SqlResult.validResultHandler(result));
	}

	@Override
	public void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		String query =
			"SELECT f.* FROM " + folderTable + " AS f " +
			"WHERE f.user_id = ? AND f.trashed = true ";

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(user.getUserId());

		sql.prepared(query, values, SqlResult.validResultHandler(result));
	}

	@Override
	public void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId))
			return;

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

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

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

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

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
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

			JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
				.add(false)
				.add(folderId)
				.add(user.getUserId())
				.add(true);

			sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void deleteFolder(String folderId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if (!deleteAll) {
			if(validationError(user, result, folderId))
				return;
		}

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		/* Get all parent folders with recursion */

		String nonRecursiveTerm =
			"SELECT DISTINCT f.* FROM " + folderTable + " AS f " +
			"WHERE ";
		JsonArray recursiveValues = new fr.wseduc.webutils.collections.JsonArray();
		if (!deleteAll) {
			nonRecursiveTerm += "f.id = ? AND ";
			recursiveValues.add(folderId);
		}
		nonRecursiveTerm += "f.user_id = ? AND f.trashed = true ";
		recursiveValues.add(user.getUserId());

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
			"WHERE ";
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		if (!deleteAll) {
			deleteFolder += "f.id = ? AND ";
			values.add(folderId);
		}
		deleteFolder += "f.user_id = ? AND f.trashed = true";
		values.add(user.getUserId());


		builder.prepared(deleteFolder, values);

		/* Perform the transaction */

		sql.transaction(builder.build(), SqlResult.validResultsHandler(result));

	}

	@Override
	public void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId))
			return;

		long attachmentSize = uploaded.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonObject attParams = new JsonObject()
			.put("id", uploaded.getString("_id"))
			.put("name", uploaded.getJsonObject("metadata").getString("name"))
			.put("filename", uploaded.getJsonObject("metadata").getString("filename"))
			.put("contentType", uploaded.getJsonObject("metadata").getString("content-type"))
			.put("contentTransferEncoding", uploaded.getJsonObject("metadata").getString("content-transfer-encoding"))
			.put("charset", uploaded.getJsonObject("metadata").getString("charset"))
			.put("size", attachmentSize);

		builder.insert(attachmentTable, attParams, "id");

		JsonObject umaParams = new JsonObject()
			.put("user_id", user.getUserId())
			.put("message_id", messageId)
			.put("attachment_id", uploaded.getString("_id"));

		builder.insert(userMessageAttachmentTable, umaParams);

		String query =
			"UPDATE " + userMessageTable + " AS um " +
			"SET total_quota = total_quota + ? " +
			"WHERE um.user_id = ? AND um.message_id = ?";
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
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

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(attachmentId)
			.add(user.getUserId())
			.add(messageId);

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if (user == null) {
			result.handle(new Either.Left<String, JsonArray>("conversation.invalid.user"));
			return;
		}
		if (messageId == null) {
			result.handle(new Either.Left<String, JsonArray>("conversation.invalid.parameter"));
			return;
		}

		String query =
			"SELECT att.* FROM " + attachmentTable + " att JOIN " +
			userMessageAttachmentTable + " uma ON uma.attachment_id = att.id " +
			"WHERE uma.user_id = ? AND uma.message_id = ?";

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(user.getUserId())
			.add(messageId);

		sql.prepared(query, values, SqlResult.validResultHandler(result));
	}

	@Override
	public void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId))
			return;

		SqlStatementsBuilder builder = new SqlStatementsBuilder();

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
			.add(messageId)
			.add(user.getUserId())
			.add(attachmentId);

		String query1 =
			"SELECT att.* FROM " + attachmentTable + " att WHERE att.id = ?";
		builder.prepared(query1, new fr.wseduc.webutils.collections.JsonArray().add(attachmentId));

		String query3 =
			"WITH attachment AS (" +
				query1 +
			") " +
			"UPDATE " + userMessageTable + " AS um " +
			"SET total_quota = um.total_quota - (SELECT SUM(DISTINCT attachment.size) FROM attachment) " +
			"WHERE um.message_id = ? AND um.user_id = ?";
		JsonArray values3 = new fr.wseduc.webutils.collections.JsonArray()
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
				} else {
					JsonArray results = event.right().getValue();
					JsonObject attachment = results.getJsonArray(0).getJsonObject(0);

					JsonObject resultJson = new JsonObject()
						.put("fileId", attachment.getString("id"))
						.put("fileSize", attachment.getLong("size"));

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

		JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
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
				additionalWhere = "AND (m.from <> ? OR m.to @> ?::jsonb OR m.cc @> ?::jsonb) AND m.state = ? AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.add(userId);
				values.add(new fr.wseduc.webutils.collections.JsonArray().add(userId).toString());
				values.add(new fr.wseduc.webutils.collections.JsonArray().add(userId).toString());
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

	private String addCompleteFolderCondition(JsonArray values, String restrain, Boolean unread, String folder, UserInfos user) {
		String additionalWhere = "";
		if(unread != null && unread){
			additionalWhere += "AND unread = ? ";
			values.add(unread);
		}
		if(restrain != null){
			additionalWhere += "AND um.folder_id = ? AND um.trashed = false";
			values.add(folder);
		} else {
			additionalWhere += addFolderCondition(folder, values, user.getUserId());
		}

		return additionalWhere;
	}

	private String addMessageConditionUnread(String folder, JsonArray values, Boolean unread, UserInfos user) {
		String messageConditionUnread = "";

		if (unread != null && unread) {
			String upFolder = folder.toUpperCase();

			// Only for user folders and trash
			if (!upFolder.equals("INBOX") && !upFolder.equals("OUTBOX") && !upFolder.equals("DRAFT")) {
				messageConditionUnread = " AND m.state = ?";
				values.add("SENT");
			}
		}

		return messageConditionUnread;
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
