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

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static io.vertx.sqlclient.Tuple.tuple;
import static org.entcore.common.user.UserUtils.findVisibles;
import static org.entcore.conversation.service.impl.ReactiveSql.validMultipleResults;
import static org.entcore.conversation.service.impl.ReactiveSql.validUniqueResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.StringValidation;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.Either.Right;

public class SqlConversationService implements ConversationService{
	private final Logger logger = LoggerFactory.getLogger(SqlConversationService.class);
	public static final int DEFAULT_SENDTIMEOUT = 15 * 60 * 1000;
	private final EventBus eb;

	private final int maxFolderDepth;

	private final String messageTable;
	private final String folderTable;
	private final String attachmentTable;
	private final String userMessageTable;
	private final String userMessageAttachmentTable;
	private final boolean optimizedThreadList;
	private int sendTimeout = DEFAULT_SENDTIMEOUT;
	private final ReactivePGClient sql;

	public SqlConversationService(Vertx vertx, String schema) {
		this.eb = Server.getEventBus(vertx);
		this.sql = new ReactivePGClient(vertx, Config.getConf());
		this.maxFolderDepth = Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH);
		messageTable = schema + ".messages";
		folderTable = schema + ".folders";
		attachmentTable = schema + ".attachments";
		userMessageTable = schema + ".usermessages";
		userMessageAttachmentTable = schema + ".usermessagesattachments";
		optimizedThreadList = vertx.getOrCreateContext().config().getBoolean("optimized-thread-list", false);
	}

	public SqlConversationService setSendTimeout(int sendTimeout) {
		this.sendTimeout = sendTimeout;
		return this;
	}

	@Override
	public void saveDraft(String parentMessageId, String threadId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		save(parentMessageId, threadId, message, user, result);
	}

	private void save(String parentMessageId, String threadId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result){
		message
			.put("id", UUID.randomUUID().toString())
			.put("from", user.getUserId())
			.put("date", System.currentTimeMillis())
			.put("state", State.DRAFT.name());

		JsonObject m = Utils.validAndGet(message, MESSAGE_FIELDS, DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result))
			return;

		if(parentMessageId != null)
			message.put("parent_id", parentMessageId);

		if(threadId != null){
			message.put("thread_id", threadId);
		}else{
			message.put("thread_id", message.getString("id"));
		}

		sql.withReadWriteTransaction(connection -> {
			// 1 - Insert message
			return sql.insert(messageTable, message, "id", connection)
				// 2 - Link message to the user
				.compose(resInsrt -> sql.insert(userMessageTable, new JsonObject()
					.put("user_id", user.getUserId())
					.put("message_id", message.getString("id")), connection)
					.map(ignored -> resInsrt));
		})
		.onSuccess(r -> validUniqueResult(r, result));
	}

	@Override
	public void updateDraft(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		update(messageId, message, user, result);
	}

	private void update(String messageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		message.put("date", System.currentTimeMillis())
				.put("from", user.getUserId());
		JsonObject m = Utils.validAndGet(message, UPDATE_DRAFT_FIELDS, UPDATE_DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result, messageId))
			return;

		StringBuilder sb = new StringBuilder();
		final Tuple values = tuple();

		for (String attr : message.fieldNames()) {
			if("to".equals(attr) || "cc".equals(attr) || "displayNames".equals(attr)){
				sb.append("\"" + attr+ "\"").append(" = CAST($" + (values.size() + 1) + "  AS JSONB),");
			} else {
				sb.append("\"" + attr+ "\"").append(" = $" + (values.size() + 1) + ",");
			}
			values.addValue(message.getValue(attr));
		}
		if(sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);

		String query =
			"UPDATE " + messageTable +
			" SET " + sb + " " +
			"WHERE id = $" + (values.size() + 1) + " AND state = $" + (values.size() + 2);
		values.addString(messageId).addString("DRAFT");

		sql.withReadWriteConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validUniqueResult(r, result));
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
			"WHERE um.user_id = $1 AND um.message_id = $2";

		final Tuple values = tuple().addString(senderId).addString(messageId);
		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validUniqueResult(r, handler));

		//sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(senderId).add(messageId), SqlResult.validUniqueResultHandler(handler, "attachmentids"));
	}

	@Override
	public void send(final String parentMessageId, final String draftId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		final String getThreadId = "SELECT thread_id FROM conversation.messages WHERE id = $1 ";
		final Tuple values = tuple().addString(draftId);
		sql.withReadOnlyConnection(connection -> sql.prepared(getThreadId, values, connection))
			.onSuccess(r -> validUniqueResult(r, either -> {
				if (either.isRight()) {
					sendMessage(parentMessageId, draftId, either.right().getValue().getString("thread_id"), message, user, result);
				} else {
					sendMessage(parentMessageId, draftId, null, message, user, result);
				}
			}))
			.onFailure(th -> {
				logger.error("An error occurred while executing a Read operation", th);
				result.handle(new Either.Left<>(th.getMessage()));
			});
	}

	private void sendMessage(final String parentMessageId, final String draftId, final String threadId, final JsonObject message, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, draftId))
			return;

		getSenderAttachments(user.getUserId(), draftId, event -> {
      if(event.isLeft()){
        result.handle(new Either.Left<>(event.left().getValue()));
        return;
      }

      JsonArray attachmentIds = event.right().getValue().getJsonArray("attachmentids");
      long totalQuota = event.right().getValue().getLong("totalquota");
      final String unread;
      final JsonArray ids = message.getJsonArray("allUsers", new fr.wseduc.webutils.collections.JsonArray());
      if(ids.contains(user.getUserId()))
        unread = "true";
			else {
				unread = "false";
			}
      sql.withReadWriteTransaction(connection -> {
        String updateMessage =
          "UPDATE " + messageTable + " SET state = $1 WHERE id = $2 "+
            "RETURNING id, subject, body, thread_id";
        String updateUnread = "UPDATE " + userMessageTable + " " +
          "SET unread = " + unread +
          " WHERE user_id = $1 AND message_id = $2 ";
				final String insertUserThread =
					"INSERT INTO conversation.userthreads as ut (user_id,thread_id,nb_unread) VALUES ($1,$2,$3) " +
						"ON CONFLICT (user_id,thread_id) DO UPDATE SET nb_unread = ut.nb_unread + 1 " +
						"WHERE ut.user_id = EXCLUDED.user_id AND ut.thread_id = EXCLUDED.thread_id";
				return sql.prepared(updateMessage, tuple().addString("SENT").addString(draftId), connection)
					.compose(r -> sql.prepared(updateUnread, tuple().addString(user.getUserId()).addString(draftId), connection)
						.compose(ignored -> {
							final String insertThread =
								"INSERT INTO conversation.threads as t (" +
									"SELECT thread_id as id, date, subject, \"from\", \"to\", cc, cci, \"displayNames\" " +
									"FROM conversation.messages m " +
									"WHERE m.id = $1) " +
									"ON CONFLICT (id) DO UPDATE SET date = EXCLUDED.date, subject = EXCLUDED.subject, \"from\" = EXCLUDED.\"from\", " +
									"\"to\" = EXCLUDED.\"to\", cc = EXCLUDED.cc, cci = EXCLUDED.cci, \"displayNames\" = EXCLUDED.\"displayNames\" " +
									"WHERE t.id = EXCLUDED.id ";
							return sql.prepared(insertThread, tuple().addString(draftId), connection);
						})
						.compose(ignored -> {
							final Future<RowSet<Row>> future;
							if (threadId == null) {
								future = Future.succeededFuture();
							} else {
								future = sql.prepared(insertUserThread, tuple().addString(user.getUserId()).addString(threadId).addInteger(0), connection);
							}
							return future;
						})
						.compose(ignored -> {
							final List<Future<?>> futures = new ArrayList<>();
							for(Object toObj : ids){
								if(toObj.equals(user.getUserId()))
									continue;
								futures.add(sql.insert(userMessageTable, new JsonObject()
									.put("user_id", toObj.toString())
									.put("message_id", draftId)
									.put("total_quota", totalQuota),
									connection));

								if (threadId != null) {
									futures.add(sql.prepared(insertUserThread, tuple().addString(toObj.toString()).addString(threadId).addInteger(1), connection));
								}

								for(Object attachmentId : attachmentIds){
									futures.add(sql.insert(userMessageAttachmentTable, new JsonObject()
										.put("user_id", toObj.toString())
										.put("message_id", draftId)
										.put("attachment_id", attachmentId.toString()),
										connection
									));
								}
							}
							return Future.all(futures);
						})
						.map(ignored -> r));
      })
      .onComplete(r -> validUniqueResult(r, result));
    });
		// sql.transaction(builder.build(),new DeliveryOptions().setSendTimeout(sendTimeout), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void list(String folder, String restrain, Boolean unread, UserInfos user, int page,final String searchText, Handler<Either<String, JsonArray>> results)
	{
		if(page < 0)
		{
			results.handle(new Either.Right<>(new JsonArray()));
			return;
		}
		int skip = page * LIST_LIMIT;
// TODO check values ordering
		final Tuple values = tuple();
		String messageConditionUnread = addMessageConditionUnread(folder, values, unread, user);
		String messagesFields = "m.id, m.subject, m.from, m.state, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\", m.cci, m.\"cciName\", m.\"displayNames\", m.date ";

		String additionalWhere = addCompleteFolderCondition(values, restrain, unread, folder, user);

		if(searchText != null){
			additionalWhere += " AND m.text_searchable  @@ to_tsquery(m.language::regconfig, unaccent(?)) ";
			values.addString(StringUtils.join(checkAndComposeWordFromSearchText(searchText), " & "));
		}
		String query = "SELECT "+messagesFields+", um.unread as unread, " +
				"CASE when COUNT(distinct r) = 0 THEN false ELSE true END AS response, COUNT(*) OVER() as count, " +
				"CASE when COUNT(distinct uma) = 0 THEN false ELSE true END AS  \"hasAttachment\" " +
				"FROM " + userMessageTable + " um LEFT JOIN " +
				userMessageAttachmentTable + " uma ON um.user_id = uma.user_id AND um.message_id = uma.message_id JOIN " +
				messageTable + " m ON (um.message_id = m.id" + messageConditionUnread + ") LEFT JOIN " +
				messageTable + " r ON um.message_id = r.parent_id AND r.from = um.user_id AND r.state= $" +(values.size() + 1)+
				" WHERE um.user_id = $"+(values.size() + 2)+ additionalWhere +
				" GROUP BY m.id, unread " +
				"ORDER BY m.date DESC LIMIT " + LIST_LIMIT + " OFFSET " + skip;
		values.addString("SENT").addString(user.getUserId());


		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, results));
		//sql.prepared(query, values, SqlResult.validResultHandler(results, "attachments", "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames"));
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

	@Override
	public void listThreads(UserInfos user, int page, Handler<Either<String, JsonArray>> results) {
		int nbThread =  10;
		int skip = page * nbThread;
		String messagesFields = "id, date, subject, \"displayNames\", \"to\", \"from\", cc, cci ";
		final Tuple values = tuple();
		values.addString(user.getUserId());
		final String query;
		if (optimizedThreadList) {
			query =
				"SELECT t.id as id, t.date as date, t.subject as subject, t.\"displayNames\" as \"displayNames\", " +
				"t.\"to\" as \"to\", t.\"from\" as \"from\", t.cc as cc, t.cci as cci, ut.nb_unread as unread " +
				"FROM conversation.userthreads ut " +
				"LEFT JOIN conversation.threads t on ut.thread_id = t.id " +
				"WHERE  ut.user_id = $1 " +
				"ORDER BY date DESC " +
				"LIMIT " + nbThread + " OFFSET " + skip;
		} else {
			query =
				" WITH threads AS ( " +
				" SELECT * from (SELECT  DISTINCT ON (m.thread_id) thread_id AS "+messagesFields+ " FROM " + userMessageTable + " um " +
				" JOIN "+messageTable+" m ON um.message_id = m.id " +
				" WHERE um.user_id = $1 AND m.state = 'SENT' AND um.trashed = false ORDER BY m.thread_id, m.date DESC) a "+
				" ORDER BY date DESC LIMIT "+ nbThread +" OFFSET "+ skip + ") " +

				"SELECT "+ messagesFields +", unread FROM threads JOIN (SELECT m.thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) AS unread " +
				"FROM threads, conversation.usermessages um JOIN conversation.messages m ON um.message_id = m.id and um.user_id= $2 " +
				"WHERE  um.trashed = false AND m.thread_id=threads.id GROUP BY m.thread_id) c ON threads.id = c.thread_id " +
				"ORDER BY date DESC";
			values.addString(user.getUserId());
		}
		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, results));
		//sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "cci", "ccName", "displayNames"));
	}

	@Override
	public void listThreadMessages(String threadId, int page, UserInfos user, Handler<Either<String, JsonArray>> results) {
		int skip = page * LIST_LIMIT;
		String messagesFields = "m.id, m.parent_id, m.subject, m.body, m.from, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\",  m.cci, m.\"cciName\", m.\"displayNames\", m.date, m.thread_id ";
		final Tuple values = tuple();

		values.addString(user.getUserId());
		values.addString(threadId);

		String query =
				"SELECT "+messagesFields+", um.unread as unread, " +
				" CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
				" FROM " +userMessageTable + " as um" +
				" JOIN "+messageTable+" as m ON um.message_id = m.id " +
				" LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
				" LEFT JOIN " + attachmentTable + " att " +
				" ON att.id = uma.attachment_id " +
				" WHERE um.user_id = $1 AND m.thread_id = $2 " +
				" AND m.state = 'SENT' AND um.trashed = false " +
				" GROUP BY m.id, um.unread " +
				" ORDER BY m.date DESC LIMIT " + LIST_LIMIT + " OFFSET " + skip;

		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, results));
		// sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames", "attachments"));
	}

	@Override
	public void listThreadMessagesNavigation(String messageId, boolean previous, UserInfos user, Handler<Either<String, JsonArray>> results) {
		int maxMessageInThread = 15;
		String messagesFields = "m.id, m.parent_id, m.subject, m.body, m.from, m.\"fromName\", m.to, m.\"toName\", m.cc, m.\"ccName\", m.cci, m.\"cciName\", m.\"displayNames\", m.date, m.thread_id ";
		String condition, limit;
		final Tuple values = tuple();

		if(previous){
			condition = " m.date < element.date ";
			limit = " LIMIT "+ maxMessageInThread +" OFFSET 0";
		}else{
			condition = " m.date > element.date ";
			limit = "";
		}
		values.addString(messageId);
		values.addString(user.getUserId());

		String query = "WITH element AS ( " +
				" SELECT thread_id, date FROM "+messageTable+" WHERE id = $1 ) " +
				" SELECT "+messagesFields+", um.unread as unread, " +
				" CASE WHEN COUNT(distinct att) = 0 THEN '[]' ELSE json_agg(distinct att.*) END AS attachments " +
				" FROM element, " +userMessageTable + " as um " +
				" JOIN "+messageTable+" as m ON um.message_id = m.id " +
				" LEFT JOIN " + userMessageAttachmentTable + " uma USING (user_id, message_id) " +
				" LEFT JOIN " + attachmentTable + " att " +
				" ON att.id = uma.attachment_id " +
				" WHERE um.user_id = $2 AND m.thread_id = element.thread_id " +
				" AND " + condition +
				" AND m.state = 'SENT' AND um.trashed = false " +
				" GROUP BY m.id, um.unread " +
				" ORDER BY m.date DESC" + limit;

		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, results));

		// sql.prepared(query, values, SqlResult.validResultHandler(results, "to", "toName", "cc", "ccName", "cci", "cciName", "displayNames", "attachments"));
	}


	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;

		final Tuple values = tuple();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " " +
			"SET trashed = true " +
			"WHERE trashed = false AND user_id = $1 AND message_id IN (");

		values.addString(user.getUserId());

		for(String id : messagesId){
			query.append("$" + (values.size() + 1) + ",");
			values.addString(id);
		}
		if(messagesId.size() > 0)
			query.deleteCharAt(query.length() - 1);
		query.append(")");

		final String deleteUserThreads =
			"DELETE FROM conversation.userthreads " +
			"WHERE user_id = $1 AND thread_id NOT IN (" +
				"SELECT DISTINCT m.thread_id " +
				"FROM conversation.usermessages um " +
				"LEFT JOIN conversation.messages m on um.message_id = m.id " +
				"WHERE user_id = $1 AND trashed = false " +
			")";
		final Tuple values2 = tuple().addString(user.getUserId());

		sql.withReadWriteTransaction(connection -> sql.prepared(query.toString(), values, connection)
			.compose(results -> sql.prepared(deleteUserThreads, values2, connection).map(results)))
			.onComplete(r -> validUniqueResult(r, result));
		//sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
	}

	@Override
	public void trashThread(List<String> threadIds, UserInfos user, Handler<Either<String, JsonObject>> result){
		final Tuple values = tuple();
		StringBuilder query = new StringBuilder(
				"UPDATE " + userMessageTable + " AS um  " +
					"SET trashed = true " +
					"FROM conversation.messages as m " +
					"WHERE m.thread_id IN ");

		query.append(generateInVars(threadIds, values));
		query.append(" AND um.user_id = $" + (values.size() + 1 ) + " AND um.trashed = false AND um.message_id = m.id ");
		values.addString(user.getUserId());

		final String deleteUserThreads =
			"DELETE FROM conversation.userthreads " +
			"WHERE thread_id IN " + ReactiveSql.listPrepared(threadIds) + " AND user_id = $" + (threadIds.size() + 1);
		final Tuple values2 = Tuple.tuple((List)threadIds).addString(user.getUserId());

		sql.withReadWriteTransaction(connection ->
			sql.prepared(query.toString(), values, connection).compose(
				r -> sql.prepared(deleteUserThreads, values2, connection).map(r)
			)
		)
		.onComplete(r -> validUniqueResult(r, result));
	}


	@Override
	public void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result)) return;

		final Tuple values = tuple();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " " +
			"SET trashed = false " +
			"WHERE trashed = true AND user_id = $1 AND message_id IN ");

		values.addString(user.getUserId());

		query.append(generateInVars(messagesId, values));

		final String insertUserThread =
				"INSERT INTO conversation.userthreads ( " +
				"SELECT um.user_id as user_id, m.thread_id as thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) as unread " +
				"FROM conversation.usermessages um " +
				"JOIN conversation.messages m on um.message_id = m.id " +
				"WHERE um.message_id IN " + ReactiveSql.listPrepared(messagesId) + " AND um.user_id = $" + (messagesId.size() + 1) + "  AND um.trashed = false AND m.state = 'SENT' " +
				"GROUP BY user_id, m.thread_id) ON CONFLICT (user_id,thread_id) DO NOTHING";
		final Tuple values2 = Tuple.tuple((List)messagesId).addString(user.getUserId());

		sql.withReadWriteTransaction(connection -> sql.prepared(query.toString(), values, connection)
			.compose(r -> sql.prepared(insertUserThread, values2, connection).map(r)))
		.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void delete(List<String> messagesId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		final Tuple values2 = tuple();
		final Tuple values3 = tuple();
		values2.addString(user.getUserId());
		values3.addString(user.getUserId());

		StringBuilder getTotalQuota = new StringBuilder(
			"SELECT coalesce(sum(um.total_quota), 0)::integer AS totalQuota FROM " + userMessageTable + " um " +
			"WHERE um.user_id = $1 AND um.trashed = true"
		);

		StringBuilder deleteUserMessages = new StringBuilder(
			"DELETE FROM " + userMessageTable + " um " +
			"WHERE um.user_id = $1 AND um.trashed = true"
		);

		if (!deleteAll) {
			getTotalQuota.append(" AND um.message_id IN ");
			getTotalQuota.append(generateInVars(messagesId, values2));
			deleteUserMessages.append(" AND um.message_id IN ");
			deleteUserMessages.append(generateInVars(messagesId, values3));
		}

		sql.withReadWriteTransaction(connection -> Future.all(
			sql.prepared(getTotalQuota.toString(), values2, connection),
			sql.prepared(deleteUserMessages.toString(), values3, connection)
		))
		.onFailure(th -> result.handle(new Either.Left<>(th.getMessage())))
		.onSuccess(results -> validMultipleResults((RowSet<Row>) results.resultAt(0), result));

	}

	@Override
	public void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId))
			return;

		String decrUnreadThread =
				"UPDATE conversation.userThreads " +
				"SET nb_unread = nb_unread - 1 " +
				"WHERE user_id = $1 AND thread_id = (" +
					"SELECT m.thread_id " +
					"FROM conversation.usermessages um " +
					"JOIN conversation.messages m on um.message_id = m.id " +
					"WHERE um.user_id = $1 AND um.message_id = $2 AND um.unread = true) " +
				"AND nb_unread > 0 ";

		String updateQuery = "UPDATE " + userMessageTable + " " +
			"SET unread = false " +
			"WHERE user_id = $1 AND message_id = $2 ";

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
			"WHERE um.user_id = $1 AND m.id = $2  " +
			"GROUP BY m.id";

		final Tuple values = tuple()
			.addString(user.getUserId())
			.addString(messageId);

		final Tuple tValues = tuple()
			.addString(user.getUserId())
			.addString(messageId);
		sql.withReadWriteTransaction(connection -> sql.prepared(decrUnreadThread, tValues, connection)
			.compose(ignored -> sql.prepared(updateQuery, values, connection))
			.compose(ignored -> sql.prepared(selectQuery, values, connection)))
		.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void count(String folder, String restrain, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folder))
			return;

		final Tuple values = tuple();

		String messageConditionUnread = addMessageConditionUnread(folder, values, unread, user);
		values.addString(user.getUserId());

		final StringBuilder query = new StringBuilder().append("SELECT count(*) as count FROM " + userMessageTable + " um JOIN " +
			messageTable + " m ON (um.message_id = m.id" + messageConditionUnread + ") " +
			"WHERE user_id = $" + values.size() + " ");

		query.append(addCompleteFolderCondition(values, restrain, unread, folder, user));

		if(restrain != null && unread){
			query.append(" AND (m.from <> $" + (values.size() + 1) + " OR m.to @> $"+(values.size() + 2)+"::jsonb OR m.cc @> $"+(values.size() + 2)+"::jsonb) ");
			values.addString(user.getUserId());
			values.addString(new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).toString());
		}

		sql.withReadOnlyConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validUniqueResult(r, result));
		//sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
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
			preFilter = "AND (m:Group OR m.displayNameSearchField CONTAINS {search}) ";
			params.put("search", StringValidation.sanitize(search));
		} else {
			preFilter = null;
		}

		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			String getMessageQuery = "SELECT m.* FROM " + messageTable +
				" WHERE id = $1";
			sql.withReadOnlyConnection(connection -> sql.prepared(getMessageQuery, tuple().addString(parentMessageId), connection))
				.onFailure(th -> {
					logger.error("An error occurred while executing a RW operation", th);
					result.handle(new Either.Left<>(th.getMessage()));
				})
				.map(r -> r.iterator().next())
				.onSuccess(r -> {
					final JsonArray to = r.getJsonArray("to");
					final JsonArray cc = r.getJsonArray("cc");

					params.put("to", to)
						.put("cc", cc);

					String customReturn =
							"MATCH (v:Visible) " +
							"WHERE (v.id = visibles.id OR v.id IN {to} OR v.id IN {cc}) " +
							"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
							"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
							"visibles.profiles[0] as profile, visibles.structureName as structureName, visibles.filter as groupProfile ";
					callFindVisibles(user, acceptLanguage, result, visible, params, preFilter, customReturn);
				}
			);
		} else {
			String customReturn =
					"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
					"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
					"visibles.profiles[0] as profile, visibles.structureName as structureName, visibles.filter as groupProfile";
			callFindVisibles(user, acceptLanguage, result, visible, params, preFilter, customReturn);
		}
	}

	private void callFindVisibles(UserInfos user, final String acceptLanguage, final Handler<Either<String, JsonObject>> result,
			final JsonObject visible, JsonObject params, String preFilter, String customReturn) {
		findVisibles(eb, user.getUserId(), customReturn, params, true, true, false, acceptLanguage, preFilter, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray visibles) {
				JsonArray users = new fr.wseduc.webutils.collections.JsonArray();
				JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
				visible.put("groups", groups).put("users", users);
				for (Object o: visibles) {
					if (!(o instanceof JsonObject)) continue;
					JsonObject j = (JsonObject) o;
					// NOTE: the management rule below is "if a visible JsonObject has a non-null *name* field, then it is a Group".
					// TODO It should be defined more clearly. See #39835
					// See also DefaultConversationService.java
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
				result.handle(new Either.Right<>(visible));
			}
		});
	}

	@Override
	public void toggleUnread(List<String> messagesIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result))
			return;
		final String getThreadIds = "SELECT thread_id FROM conversation.messages WHERE id IN " + ReactiveSql.listPrepared(messagesIds);
		final Tuple values = tuple((List)messagesIds);
		sql.withReadWriteConnection(connection -> sql.prepared(getThreadIds, values, connection))
		.onFailure(th -> {
			logger.error("An error occurred while executing a RW operation", th);
			result.handle(new Either.Left<>(th.getMessage()));
		})
		.onSuccess(r -> validMultipleResults(r, either -> {
			if (either.isRight()) {
				final Tuple updateValues = tuple();
				String query = "UPDATE " + userMessageTable + " " +
						"SET unread = $1 " +
						"WHERE user_id = $2 AND message_id IN "  + ReactiveSql.listPrepared(messagesIds, 3);

				updateValues.addBoolean(unread);
				updateValues.addString(user.getUserId());
				for(String id : messagesIds){
					updateValues.addString(id);
				}

				sql.withReadWriteConnection(connection ->
					sql.prepared(query, updateValues, connection)
						.compose(resultsUpdate -> {
							final List<String> threadIds = new ArrayList<>();
							for (Object row: either.right().getValue()) {
								if (!(row instanceof JsonObject)) continue;
								threadIds.add(((JsonObject) row).getString("thread_id"));
							}
							return recalculateNbUnreadInThreads(threadIds, user, connection).map(resultsUpdate);
						})
				).onComplete(updateResults -> validUniqueResult(updateResults, result));
				//sql.transaction(builder.build(), SqlResult.validUniqueResultHandler(0, result));
			} else {
				result.handle(new Either.Left<>(either.left().getValue()));
			}
		}));
	}

	@Override
	public void toggleUnreadThread(List<String> threadIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		final Tuple values = tuple();
		StringBuilder query = new StringBuilder(
				"UPDATE " + userMessageTable + " AS um  " +
						"SET  unread = $1 " +
						"FROM conversation.messages as m " +
						"WHERE m.thread_id IN ");
		values.addBoolean(unread);
		query.append(generateInVars(threadIds, values));
		query.append(" AND um.user_id = $" + (values.size() + 1)+ " AND um.message_id = m.id ");
		values.addString(user.getUserId());

		sql.withReadWriteTransaction(connection -> sql.prepared(query.toString(), values, connection)
			.compose(r -> recalculateNbUnreadInThreads(threadIds, user, connection).map(r)))
		.onComplete(r -> validUniqueResult(r, result));
	}

	private Future<RowSet<Row>> recalculateNbUnreadInThreads(List<String> threadIds, UserInfos user, SqlConnection connection) {
		Tuple values = Tuple.tuple((List)threadIds).addString(user.getUserId());
		final String query =
				"UPDATE conversation.userthreads as ut " +
				"SET nb_unread = g.unread " +
				"FROM (" +
					"SELECT um.user_id as user_id, m.thread_id as thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) as unread " +
					"FROM conversation.usermessages um " +
					"JOIN conversation.messages m on um.message_id = m.id " +
					"WHERE m.thread_id IN " + ReactiveSql.listPrepared(threadIds) + " AND um.user_id = $"+values.size()+" AND m.state = 'SENT' " +
					"GROUP BY user_id, m.thread_id " +
				") as g " +
				"WHERE ut.user_id = g.user_id AND ut.thread_id = g.thread_id ";
		return sql.prepared(query, values, connection);
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

		final JsonObject messageObj = new JsonObject()
			.put("id", id)
			.put("name", folderName)
			.put("user_id", user.getUserId());

		if (parentFolderId != null) {
			final Tuple values = tuple().addString(user.getUserId()).addString(parentFolderId);
			final String depthQuery = "SELECT depth FROM " + folderTable + " WHERE user_id = $1 AND id = $2";
			sql.withReadWriteTransaction(connection -> sql.prepared(depthQuery, values, connection)
				.compose(r -> {
					final Future<RowSet<Row>> future;
					if(r.rowCount() == 1) {
						int parentDepth = r.iterator().next().getInteger("depth");
						if (parentDepth >= maxFolderDepth) {
							future = Future.failedFuture("error.max.folder.depth");
						} else {
							messageObj
								.put("parent_id", parentFolderId)
								.put("depth", parentDepth + 1);
							future = sql.insert(folderTable, messageObj, connection);
						}
					} else {
						future = Future.failedFuture("no.rows.returned");
					}
					return future;
				})
				.onComplete(r -> validUniqueResult(r, resultOriginal)));
		} else {
			sql.withReadWriteConnection(connection -> sql.insert(folderTable, messageObj, connection))
				.onComplete(r -> validUniqueResult(r, result));
		}

	}

	@Override
	public void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> resultOriginal) {
		if (validationParamsError(user, resultOriginal, data.getString("name")))
			return;
		final Handler<Either<String, JsonObject>> result = res->{
			if(res.isLeft()){
				if(isDuplicateError(res.left().getValue())){
					resultOriginal.handle(new Either.Left<>("conversation.error.duplicate.folder"));
				}else{
					resultOriginal.handle(res.left());
				}
			}else{
				resultOriginal.handle(res.right());
			}
		};
		String query = "UPDATE " + folderTable + " AS f " +
			"SET name = $1, skip_uniq=FALSE " +
			"WHERE f.id = $2 AND f.user_id = $3";

		final Tuple values = tuple()
			.addString(data.getString("name"))
			.addString(folderId)
			.addString(user.getUserId());
		sql.withReadWriteConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validUniqueResult(r, result));

		// sql.prepared(query, values, SqlResult.validUniqueResultHandler(result));
	}

	@Override
	public void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		final StringBuilder query = new StringBuilder("SELECT f.* FROM " + folderTable + " AS f WHERE f.user_id = $1 AND f.trashed = false ");

		final Tuple values = tuple().addString(user.getUserId());

		if(parentId == null){
			query.append("AND f.parent_id IS NULL");
		} else {
			query.append("AND f.parent_id = $2");
			values.addString(parentId);
		}

		sql.withReadWriteConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> ReactiveSql.validMultipleResults(r, result));
		// sql.prepared(query, values, SqlResult.validResultHandler(result));
	}


	@Override
	public void listUserFolders(Optional<String>  parentId, UserInfos user, Boolean unread, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;
		final Tuple values = tuple();
		final StringBuilder subQuery = new StringBuilder();
		subQuery.append("SELECT count(*) as count,um.folder_id  FROM ").append(userMessageTable).append(" um ");
		subQuery.append(" INNER JOIN ").append(messageTable).append(" m ON (um.message_id = m.id) ");
		subQuery.append(" WHERE um.user_id = $1  AND m.state='SENT' ");
		subQuery.append(" AND (m.from <> $1 OR m.to @> $2::jsonb OR m.cc @> $2::jsonb) ");
		values.addString(user.getUserId());
		values.addString(new JsonArray().add(user.getUserId()).toString());
		if(unread != null && unread){
			subQuery.append(" AND um.unread = ").append(unread ? " TRUE " : " FALSE ");
		}
		subQuery.append(" GROUP BY um.folder_id ");
		//values SENT
		final StringBuilder query = new StringBuilder();
		query.append("SELECT f.*, COALESCE(sub.count,0) as \"nbUnread\" FROM ").append(folderTable).append(" AS f ");
		query.append("LEFT JOIN (").append(subQuery).append(") AS sub ON (f.id=sub.folder_id) ");
		query.append("WHERE f.user_id = $1 AND f.trashed IS FALSE ");
		if(parentId.isPresent()){
			query.append(" AND f.parent_id = $3 ");
			values.addString(parentId.get());
		}else{
			query.append(" AND f.parent_id IS NULL ");
		}

		sql.withReadOnlyConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validMultipleResults(r, result));
		//sql.prepared(query.toString(), values, SqlResult.validResultHandler(result));
	}

	@Override
	public void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result))
			return;

		String query =
			"SELECT f.* FROM " + folderTable + " AS f " +
			"WHERE f.user_id = $1 AND f.trashed = true ";

		final Tuple values = tuple().addString(user.getUserId());

		sql.withReadOnlyConnection(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, result));
		//sql.prepared(query, values, SqlResult.validResultHandler(result));
	}

	@Override
	public void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId))
			return;

		final Tuple values = tuple();

		final StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " AS um " +
			"SET folder_id = $1 " +
			"WHERE um.user_id = $2 AND um.message_id IN ");

		values
			.addString(folderId)
			.addString(user.getUserId());

		query.append(generateInVars(messageIds, values));

		sql.withReadWriteConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void backToSystemFolder(List<String> messageIds, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result))
			return;

		final Tuple values = tuple();

		StringBuilder query = new StringBuilder(
			"UPDATE " + userMessageTable + " AS um " +
			"SET folder_id = NULL " +
			"WHERE um.user_id = $1 AND um.message_id IN ");

		values.addString(user.getUserId());

		query.append(generateInVars(messageIds, values));
		sql.withReadWriteConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void trashFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String query =
			"UPDATE " + folderTable + " AS f " +
			"SET trashed = $1 " +
			"WHERE f.id = $2 AND f.user_id = $3 AND f.trashed = $4";

		final Tuple values = tuple()
			.addBoolean(true)
			.addString(folderId)
			.addString(user.getUserId())
			.addBoolean(false);

		sql.withReadWriteConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void restoreFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String query =
			"UPDATE " + folderTable + " AS f " +
			"SET trashed = $1 " +
			"WHERE f.id = $2 AND f.user_id = $3 AND f.trashed = $4";

			final Tuple values = tuple()
				.addBoolean(false)
				.addString(folderId)
				.addString(user.getUserId())
				.addBoolean(true);

		sql.withReadWriteConnection(connection -> sql.prepared(query.toString(), values, connection))
			.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void deleteFolder(String folderId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if (!deleteAll) {
			if(validationError(user, result, folderId))
				return;
		}

		/* Get all parent folders with recursion */

		String nonRecursiveTerm =
			"SELECT DISTINCT f.* FROM " + folderTable + " AS f " +
			"WHERE ";
		final Tuple recursivevalues = Tuple.tuple();
		int idxParam = 1;
		if (!deleteAll) {
			nonRecursiveTerm += "f.id = $1 AND ";
			recursivevalues.addString(folderId);
			idxParam++;
		}
		nonRecursiveTerm += "f.user_id = "+(idxParam++)+" AND f.trashed = true ";
		recursivevalues.addString(user.getUserId());

		String recursiveTerm =
			"SELECT f.* FROM " + folderTable + " AS f JOIN " +
			"parents ON f.parent_id = parents.id " +
			"WHERE f.user_id = $"+idxParam;
		recursivevalues.addString(user.getUserId());

		/* Get quota to free */

		String quotaRecursion =
			"WITH RECURSIVE parents AS ( "+
					nonRecursiveTerm +
					"UNION " +
					recursiveTerm +
			") " +
			"SELECT COALESCE(sum(um.total_quota), 0)::integer AS totalQuota FROM parents JOIN " +
			userMessageTable + " um ON um.folder_id = parents.id AND um.user_id = parents.user_id ";

		sql.withReadWriteConnection(connection -> sql.prepared(quotaRecursion, recursivevalues, connection)
			.compose(r -> {
				/* Physically delete the folder, which will start a cascading delete process for parent folders, messages and attachments. */
				String deleteFolder = "DELETE FROM " + folderTable + " f WHERE f.user_id = $1 AND f.trashed = true ";
				final Tuple values = tuple().addString(user.getUserId());
				if (!deleteAll) {
					deleteFolder += " AND f.id = $2";
					values.addString(folderId);
				}
				return sql.prepared(deleteFolder, values, connection)
					.map(rDelete -> Lists.newArrayList(r, rDelete));
			}))
			.onComplete(r -> ReactiveSql.validListOfMultipleResults((AsyncResult)r, result));
	}

	@Override
	public void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId))
			return;

		long attachmentSize = uploaded.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);

		JsonObject attParams = new JsonObject()
			.put("id", uploaded.getString("_id"))
			.put("name", uploaded.getJsonObject("metadata").getString("name"))
			.put("filename", uploaded.getJsonObject("metadata").getString("filename"))
			.put("contentType", uploaded.getJsonObject("metadata").getString("content-type"))
			.put("contentTransferEncoding", uploaded.getJsonObject("metadata").getString("content-transfer-encoding"))
			.put("charset", uploaded.getJsonObject("metadata").getString("charset"))
			.put("size", attachmentSize);

		sql.withReadWriteTransaction(connection -> sql.insert(attachmentTable, attParams, "id", connection)
			.compose(r -> {
				JsonObject umaParams = new JsonObject()
					.put("user_id", user.getUserId())
					.put("message_id", messageId)
					.put("attachment_id", uploaded.getString("_id"));

				return sql.insert(userMessageAttachmentTable, umaParams, connection)
					.compose(ignored -> {
						String query =
							"UPDATE " + userMessageTable + " AS um " +
								"SET total_quota = total_quota + $1 " +
								"WHERE um.user_id = $2 AND um.message_id = $3";
						final Tuple values = tuple()
							.addLong(attachmentSize)
							.addString(user.getUserId())
							.addString(messageId);
						return sql.prepared(query, values, connection);
					}).map(r);
			}))
		.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId))
			return;

		String query =
			"SELECT att.* FROM " + attachmentTable + " att JOIN " +
			userMessageAttachmentTable + " uma ON uma.attachment_id = att.id " +
			"WHERE att.id = $1 AND uma.user_id = $2 AND uma.message_id = $3";

		final Tuple values = tuple()
			.addString(attachmentId)
			.addString(user.getUserId())
			.addString(messageId);

		sql.withReadOnlyTransaction(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validUniqueResult(r, result));
	}

	@Override
	public void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if (user == null) {
			result.handle(new Either.Left<>("conversation.invalid.user"));
			return;
		}
		if (messageId == null) {
			result.handle(new Either.Left<>("conversation.invalid.parameter"));
			return;
		}

		String query =
			"SELECT att.* FROM " + attachmentTable + " att JOIN " +
			userMessageAttachmentTable + " uma ON uma.attachment_id = att.id " +
			"WHERE uma.user_id = $1 AND uma.message_id = $2";

		final Tuple values = tuple()
			.addString(user.getUserId())
			.addString(messageId);

		sql.withReadOnlyTransaction(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validMultipleResults(r, result));
	}

	@Override
	public void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId))
			return;

		String query1 =
			"SELECT att.* FROM " + attachmentTable + " att WHERE att.id = $1";

		sql.withReadWriteTransaction(connection ->
			sql.prepared(query1, tuple().addString(attachmentId), connection)
			.compose(r -> {
				final String query3 =
					"WITH attachment AS (" +
						query1 +
						") " +
						"UPDATE " + userMessageTable + " AS um " +
						"SET total_quota = um.total_quota - (SELECT SUM(DISTINCT attachment.size) FROM attachment) " +
						"WHERE um.message_id = $2 AND um.user_id = $3";
				final Tuple values3 = Tuple.tuple()
					.addString(attachmentId)
					.addString(messageId)
					.addString(user.getUserId());
				return sql.prepared(query3, values3, connection).map(r);
			})
			.compose(r -> {
				final String query4 =
					"DELETE FROM " + userMessageAttachmentTable + " WHERE " +
						"message_id = $1 AND user_id = $2 AND attachment_id = $3";
				final Tuple values = tuple()
					.addString(messageId)
					.addString(user.getUserId())
					.addString(attachmentId);
				return sql.prepared(query4, values, connection);
		}))
		.onFailure(th -> new Either.Left<>(th.getMessage()))
		.onSuccess(r -> {
			JsonObject attachment = (JsonObject) r.iterator().next().getValue(0);
			JsonObject resultJson = new JsonObject()
				.put("fileId", attachment.getString("id"))
				.put("fileSize", attachment.getLong("size"));
			result.handle(new Either.Right<>(resultJson));
		});
	}

	@Override
	public void forwardAttachments(String forwardId, String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId))
			return;

		String query =
			"WITH messageAttachments AS (" +
				"SELECT * FROM " + userMessageAttachmentTable + " " +
				"WHERE user_id = $1 AND message_id = $2" +
			") " +
			"INSERT INTO " + userMessageAttachmentTable + " " +
			"SELECT user_id, $3 AS message_id, attachment_id FROM messageAttachments";

		final Tuple values = tuple()
				.addString(user.getUserId())
				.addString(forwardId)
				.addString(messageId);

		sql.withReadWriteTransaction(connection -> sql.prepared(query, values, connection))
			.onComplete(r -> validUniqueResult(r, result));
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

	private String generateInVars(List<String> list, final Tuple values){
		StringBuilder builder = new StringBuilder();
		builder.append("(");

		for(String item : list){
			builder.append("$" + (values.size() + 1) + ",");
			values.addString(item);
		}
		if(list.size() > 0)
			builder.deleteCharAt(builder.length() - 1);
		builder.append(")");

		return builder.toString();
	}

	private String addFolderCondition(String folder, Tuple values, String userId){
		String additionalWhere = "";
		switch(folder.toUpperCase()){
			case "INBOX":
				additionalWhere = "AND (m.from <> $"+(values.size() + 1)+" OR m.to @> $"+(values.size() + 2)+"::jsonb OR m.cc @> $"+(values.size() + 2)+"::jsonb) AND m.state = $"+(values.size() + 3)+" AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.addString(userId);
				values.addString(new fr.wseduc.webutils.collections.JsonArray().add(userId).toString());
				values.addString("SENT");
				break;
			case "OUTBOX":
				additionalWhere = "AND m.from = $"+(values.size() + 1)+" AND m.state = $"+(values.size() + 2)+" AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.addString(userId);
				values.addString("SENT");
				break;
			case "DRAFT":
				additionalWhere = "AND m.from = $"+(values.size() + 1)+" AND m.state = $"+(values.size() + 2)+" AND um.trashed = false";
				additionalWhere += " AND um.folder_id IS NULL";
				values.addString(userId);
				values.addString("DRAFT");
				break;
			case "TRASH":
				additionalWhere = "AND um.trashed = true";
				break;
		}
		return additionalWhere;
	}

	private String addCompleteFolderCondition(final Tuple values, String restrain, Boolean unread, String folder, UserInfos user) {
		String additionalWhere = "";
		if(unread != null && unread){
			additionalWhere += " AND unread = $" +(values.size() + 1)+ " ";
			values.addBoolean(unread);
		}
		if(restrain != null){
			additionalWhere += " AND um.folder_id = $"+(values.size() + 1)+" AND um.trashed = false ";
			values.addString(folder);
		} else {
			additionalWhere += addFolderCondition(folder, values, user.getUserId());
		}

		return additionalWhere;
	}

	private String addMessageConditionUnread(String folder, final Tuple values, Boolean unread, UserInfos user) {
		String messageConditionUnread = "";

		if (unread != null && unread) {
			String upFolder = folder.toUpperCase();

			// Only for user folders and trash
			if (!upFolder.equals("INBOX") && !upFolder.equals("OUTBOX") && !upFolder.equals("DRAFT")) {
				messageConditionUnread = " AND m.state = $" + (values.size() + 1) ;
				values.addString("SENT");
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
			result.handle(new Either.Left<>("conversation.invalid.fields"));
			return true;
		}
		return validationParamsError(user, result, params);
	}

}
