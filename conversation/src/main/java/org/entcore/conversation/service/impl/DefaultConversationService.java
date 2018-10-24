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

package org.entcore.conversation.service.impl;

import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.UserUtils.findVisibleUsers;
import static org.entcore.common.user.UserUtils.findVisibles;
import fr.wseduc.webutils.Server;

import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.conversation.Conversation;
import org.entcore.conversation.service.ConversationService;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

public class DefaultConversationService implements ConversationService {

	private final EventBus eb;
	private final Neo neo;
	private final String applicationName;
	private final int maxFolderDepth;

	public DefaultConversationService(Vertx vertx, String applicationName) {
		eb = Server.getEventBus(vertx);
		neo = new Neo(vertx, eb, LoggerFactory.getLogger(Neo.class));
		this.applicationName = applicationName;
		this.maxFolderDepth = Config.getConf().getInteger("max-folder-depth", Conversation.DEFAULT_FOLDER_DEPTH);
	}

	@Override
	public void saveDraft(final String parentMessageId, final String threadId, final JsonObject message,
			final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if (displayNamesCondition(message)) {
			addDisplayNames(message, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject object) {
					save(parentMessageId, object, user, result);
				}
			});
		} else {
			save(parentMessageId, message, user, result);
		}
	}

	private void addDisplayNames(final JsonObject message, final Handler<JsonObject> handler) {
		String query =
				"MATCH (v:Visible) " +
				"WHERE v.id IN {ids} " +
				"RETURN COLLECT(distinct (v.id + '$' + coalesce(v.displayName, ' ') + '$' + " +
				"coalesce(v.name, ' ') + '$' + coalesce(v.groupDisplayName, ' '))) as displayNames ";
		Set<String> ids = new HashSet<>();
		ids.addAll(message.getJsonArray("to", new fr.wseduc.webutils.collections.JsonArray()).getList());
		ids.addAll(message.getJsonArray("cc", new fr.wseduc.webutils.collections.JsonArray()).getList());
		if (message.containsKey("from")) {
			ids.add(message.getString("from"));
		}
		neo.execute(query, new JsonObject().put("ids", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(ids))),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray r = m.body().getJsonArray("result");
				if ("ok".equals(m.body().getString("status")) && r != null && r.size() == 1) {
					JsonObject j = r.getJsonObject(0);
					JsonArray d = j.getJsonArray("displayNames");
					if (d != null && d.size() > 0) {
						message.put("displayNames", d);
					}
				}
				handler.handle(message);
			}
		});
	}

	private boolean displayNamesCondition(JsonObject message) {
		return message != null && (
				(message.containsKey("from") && !message.getString("from").trim().isEmpty()) ||
				(message.containsKey("to") && message.getJsonArray("to").size() > 0) ||
				(message.containsKey("cc") && message.getJsonArray("cc").size() > 0));
	}

	private void save(String parentMessageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result) {
		if (message == null) {
			message = new JsonObject();
		}
		message.put("id", UUID.randomUUID().toString())
				.put("date", System.currentTimeMillis())
				.put("from", user.getUserId())
				.put("state", State.DRAFT.name());
		JsonObject m = Utils.validAndGet(message, MESSAGE_FIELDS, DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result)) return;
		String query;
		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("folderName", "DRAFT")
				.put("props", m)
				.put("true", true);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) { // reply
			query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder), " +
				"(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(af:ConversationSystemFolder)" +
				"-[:HAS_CONVERSATION_MESSAGE]->(pm:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folderName} " +
				"AND pm.id = {parentMessageId} " +
				"WITH distinct f, pm, count(distinct f) as nb " +
				"WHERE nb = 1 " +
				"CREATE f-[:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage {props})" +
				"-[:PARENT_CONVERSATION_MESSAGE]->pm " +
				"RETURN m.id as id";
			params.put("parentMessageId", parentMessageId);
		} else {
			query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folderName} " +
				"WITH f, count(f) as nb " +
				"WHERE nb = 1 " +
				"CREATE f-[:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage {props}) " +
				"RETURN m.id as id";
		}
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void updateDraft(final String messageId, final JsonObject message,
			final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if (displayNamesCondition(message)) {
			addDisplayNames(message, new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject object) {
					update(messageId, object, user, result);
				}
			});
		} else {
			update(messageId, message, user, result);
		}
	}

	private void update(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result) {
		if (message == null) {
			message = new JsonObject();
		}
		message.put("date", System.currentTimeMillis());
		JsonObject m = Utils.validAndGet(message, UPDATE_DRAFT_FIELDS, UPDATE_DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result, messageId)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
				"<-[r:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {id} AND m.from = {userId} AND m.state = {state} AND c.active = {true} " +
				"SET " + nodeSetPropertiesFromJson("m", m) +
				"RETURN m.id as id";
		m.put("userId", user.getUserId())
				.put("id", messageId)
				.put("state", State.DRAFT.name())
				.put("true", true);
		neo.execute(query, m, validUniqueResultHandler(result));
	}

	@Override
	public void send(final String parentMessageId, final String draftId, JsonObject message,
			final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		Handler<Either<String, JsonObject>> handler = new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (draftId != null && !draftId.trim().isEmpty()) {
					send(parentMessageId, draftId, user, result);
				} else if (event.isRight()) {
					send(parentMessageId, event.right().getValue().getString("id"), user, result);
				} else {
					result.handle(event);
				}
			}
		};
		if (draftId != null && !draftId.trim().isEmpty()) {
			update(draftId, message, user, handler);
		} else {
			save(parentMessageId, message, user, handler);
		}
	}

	private void send(final String parentMessageId, final String messageId, final UserInfos user,
			final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId)) return;

		String attachmentsRetrieval =
			"MATCH (message:ConversationMessage)<-[link:HAS_CONVERSATION_MESSAGE]-(:ConversationSystemFolder)" +
			"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
			"WHERE message.id = {messageId} AND message.state = {draft} AND message.from = {userId} AND c.userId = {userId} AND c.active = {true} " +
			"OPTIONAL MATCH (message)-[:HAS_ATTACHMENT]->(a: MessageAttachment) " +
			"WHERE a.id IN link.attachments " +
			"WITH CASE WHEN a IS NULL THEN [] ELSE collect({id: a.id, size: a.size}) END as attachments " +
			"RETURN attachments";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("messageId", messageId)
			.put("draft", State.DRAFT.name())
			.put("true", true);

		neo.execute(attachmentsRetrieval, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
				public void handle(Either<String, JsonObject> event) {
					if(event.isLeft() || event.isRight() && event.right().getValue() == null){
						result.handle(new Either.Left<String, JsonObject>("conversation.send.error"));
						return;
					}

					JsonArray attachments = event.right().getValue().getJsonArray("attachments", new fr.wseduc.webutils.collections.JsonArray());

					if(attachments.size() < 1){
						sendWithoutAttachments(parentMessageId, messageId, user, result);
					} else {
						sendWithAttachments(parentMessageId, messageId, attachments, user, result);
					}

				}
			})
		);
	}

	private void sendWithoutAttachments(final String parentMessageId, String messageId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		String usersQuery;
		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("messageId", messageId)
				.put("draft", State.DRAFT.name())
				.put("outbox", "OUTBOX")
				.put("inbox", "INBOX")
				.put("sent", State.SENT.name())
				.put("true", true);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) { // reply
			usersQuery =
				"MATCH (m:ConversationMessage { id : {parentMessageId}}) " +
				"WITH (COLLECT(visibles.id) + coalesce(m.to, '') + coalesce(m.cc, '') + coalesce(m.from, '')) as vis " +
				"MATCH (v:Visible) " +
				"WHERE v.id IN vis " +
				"WITH DISTINCT v ";
			params.put("parentMessageId", parentMessageId);
		} else {
			usersQuery = "WITH visibles as v ";
		}
		String query =
			usersQuery +
			"MATCH v<-[:IN*0..1]-(u:User), (message:ConversationMessage) " +
			"WHERE (v: User or v:Group) " +
			"AND message.id = {messageId} AND message.state = {draft} AND message.from = {userId} AND " +
			"(v.id IN message.to OR v.id IN message.cc) " +
			"WITH DISTINCT u, message, (v.id + '$' + coalesce(v.displayName, ' ') + '$' + " +
			"coalesce(v.name, ' ') + '$' + coalesce(v.groupDisplayName, ' ')) as dNames " +
			"MATCH u-[:HAS_CONVERSATION]->(c:Conversation {active:{true}})" +
			"-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder {name:{inbox}}) " +
			"CREATE UNIQUE f-[:HAS_CONVERSATION_MESSAGE { unread: {true} }]->message " +
			"WITH COLLECT(c.userId) as sentIds, COLLECT(u) as users, message, " +
			"COLLECT(distinct dNames) as displayNames " +
			"MATCH (s:User {id : {userId}})-[:HAS_CONVERSATION]->(:Conversation)" +
			"-[:HAS_CONVERSATION_FOLDER]->(fOut:ConversationSystemFolder {name : {outbox}}), " +
			"message<-[r:HAS_CONVERSATION_MESSAGE]-(fDraft:ConversationSystemFolder {name : {draft}}) " +
			"SET message.state = {sent}, " +
			"message.displayNames = displayNames + (s.id + '$' + coalesce(s.displayName, ' ') + '$ $ ') " +
			"CREATE UNIQUE fOut-[:HAS_CONVERSATION_MESSAGE { insideFolder: r.insideFolder }]->message " +
			"DELETE r " +
			"RETURN EXTRACT(u IN FIlTER(x IN users WHERE NOT(x.id IN sentIds)) | u.displayName) as undelivered,  " +
			"EXTRACT(u IN FIlTER(x IN users WHERE x.id IN sentIds AND NOT(x.activationCode IS NULL)) " +
			"| u.displayName) as inactive, LENGTH(sentIds) as sent, " +
			"sentIds, message.id as id, message.subject as subject";
		findVisibles(eb, user.getUserId(), query, params, true, true, false, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray event) {
				if (event != null && event.size() == 1 && (event.getValue(0) instanceof JsonObject)) {
					result.handle(new Either.Right<String, JsonObject>(event.getJsonObject(0)));
				} else {
					result.handle(new Either.Left<String, JsonObject>("conversation.send.error"));
				}
			}
		});
	}

	private void sendWithAttachments(final String parentMessageId, final String messageId, JsonArray attachments, final UserInfos user, final Handler<Either<String, JsonObject>> result) {
		long totalAttachmentsSize = 0l;
		for(Object o : attachments){
			totalAttachmentsSize = totalAttachmentsSize + ((JsonObject) o).getLong("size", 0l);
		}

		final String usersQuery;
		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("messageId", messageId)
				.put("draft", State.DRAFT.name())
				.put("outbox", "OUTBOX")
				.put("inbox", "INBOX")
				.put("sent", State.SENT.name())
				.put("attachmentsSize", totalAttachmentsSize)
				.put("true", true);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) { // reply
			usersQuery =
				"MATCH (m:ConversationMessage { id : {parentMessageId}}) " +
				"WITH (COLLECT(visibles.id) + coalesce(m.to, '') + coalesce(m.cc, '') + coalesce(m.from, '')) as vis " +
				"MATCH (v:Visible) " +
				"WHERE v.id IN vis " +
				"WITH DISTINCT v ";
			params.put("parentMessageId", parentMessageId);
		} else {
			usersQuery = "WITH visibles as v ";
		}

		String query =
			usersQuery +
			"MATCH v<-[:IN*0..1]-(u:User), (message:ConversationMessage)-[:HAS_ATTACHMENT]->(attachment: MessageAttachment) " +
			"WHERE (v: User or v:Group) " +
			"AND message.id = {messageId} AND message.state = {draft} AND message.from = {userId} AND " +
			"(v.id IN message.to OR v.id IN message.cc) " +
			"WITH DISTINCT u, message, (v.id + '$' + coalesce(v.displayName, ' ') + '$' + " +
			"coalesce(v.name, ' ') + '$' + coalesce(v.groupDisplayName, ' ')) as dNames, COLLECT(distinct attachment.id) as attachments " +
			"MATCH (ub: UserBook)<-[:USERBOOK]-(u)-[:HAS_CONVERSATION]->(c:Conversation {active:{true}})" +
			"-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder {name:{inbox}}) " +
			"WHERE (ub.quota - ub.storage) >= {attachmentsSize} " +
			"CREATE UNIQUE f-[:HAS_CONVERSATION_MESSAGE { unread: {true}, attachments: attachments }]->message " +
			"SET ub.storage = ub.storage + {attachmentsSize} " +
			"WITH message, COLLECT(c.userId) as sentIds, COLLECT(distinct u) as users, " +
			"COLLECT(distinct dNames) as displayNames " +
			"MATCH (s:User {id : {userId}})-[:HAS_CONVERSATION]->(:Conversation)" +
			"-[:HAS_CONVERSATION_FOLDER]->(fOut:ConversationSystemFolder {name : {outbox}}), " +
			"message<-[r:HAS_CONVERSATION_MESSAGE]-(fDraft:ConversationSystemFolder {name : {draft}}) " +
			"SET message.state = {sent}, " +
			"message.displayNames = displayNames + (s.id + '$' + coalesce(s.displayName, ' ') + '$ $ ') " +
			"CREATE UNIQUE fOut-[:HAS_CONVERSATION_MESSAGE { insideFolder: r.insideFolder, attachments: r.attachments }]->message " +
			"DELETE r " +
			"RETURN sentIds, message.id as id";

		findVisibles(eb, user.getUserId(), query, params, true, true, false, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray event) {
				if (event != null && event.size() == 1 && (event.getValue(0) instanceof JsonObject)) {

					JsonObject resultObj = event.getJsonObject(0);
					JsonArray sentIds = resultObj.getJsonArray("sentIds");
					String messageId = resultObj.getString("id");

					String query = usersQuery +
						"MATCH v<-[:IN*0..1]-(u:User), (message:ConversationMessage) " +
						"WHERE (v: User or v:Group) " +
						"AND message.id = {messageId} AND message.from = {userId} AND " +
						"(v.id IN message.to OR v.id IN message.cc) " +
						"RETURN EXTRACT(user IN FIlTER(x IN COLLECT(u) WHERE NOT(x.id IN {sentIds}))|user.displayName) as undelivered, {sentIds} as sentIds, [] as inactive, " +
						"{sentIdsLength} as sent, message.id as id, message.subject as subject";

					JsonObject params = new JsonObject()
						.put("userId", user.getUserId())
						.put("messageId", messageId)
						.put("sentIds", sentIds)
						.put("sentIdsLength", sentIds.size());
					if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
						params.put("parentMessageId", parentMessageId);
					}

					findVisibles(eb, user.getUserId(), query, params, true, true, false, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray event) {
							if (event != null && event.size() == 1 && (event.getValue(0) instanceof JsonObject)) {
								result.handle(new Either.Right<String, JsonObject>(event.getJsonObject(0)));
							} else {
								result.handle(new Either.Left<String, JsonObject>("conversation.send.error"));
							}
						}
					});
				} else {
					result.handle(new Either.Left<String, JsonObject>("conversation.send.error"));
				}
			}
		});
	}

	@Override
	public void list(String folder, String restrain, Boolean unread, UserInfos user, int page, String searchWords, final Handler<Either<String, JsonArray>> results) {
		if (validationError(user, results, folder)) return;
		int skip = page * LIST_LIMIT;

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("folder", folder)
			.put("skip", skip)
			.put("limit", LIST_LIMIT)
			.put("true", true);

		String messageFilter = "";
		if(restrain != null){
			messageFilter =
				messageFilter +
				"-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)" +
				"-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(child: ConversationUserFolder {id: {userFolderId}})" +
				"<-[i: INSIDE]-(m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f: ConversationSystemFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
				"WHERE NOT HAS(i.trashed) ";

			params.put("userFolderId", folder);
		} else {
			messageFilter =
				messageFilter +
				"-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder {name : {folder}})" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
				"WHERE NOT HAS(r.insideFolder) ";
		}

		String query =
				"MATCH (c:Conversation {userId : {userId}, active : {true}})" +
				messageFilter +
				"RETURN DISTINCT m.id as id, m.to as to, m.from as from, m.state as state, " +
				"m.toName as toName, m.fromName as fromName, " +
				"m.subject as subject, m.date as date, r.unread as unread, m.displayNames as displayNames, coalesce(r.attachments, []) as attachments,  collect(f.name) as systemFolders " +
				"ORDER BY m.date DESC " +
				"SKIP {skip} " +
				"LIMIT {limit} ";

		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void listThreads(UserInfos user, int page, Handler<Either<String, JsonArray>> results) {

	}

	@Override
	public void listThreadMessages(String threadId, int page, UserInfos user, Handler<Either<String, JsonArray>> results) {

	}

	@Override
	public void listThreadMessagesNavigation(String messageId, boolean previous, UserInfos user, Handler<Either<String, JsonArray>> results) {

	}

	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;

		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->" +
				"(df:ConversationSystemFolder) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND df.name = {trash} " +
				"CREATE UNIQUE df-[:HAS_CONVERSATION_MESSAGE { restoreFolder: f.name, wasInsideFolder: r.insideFolder, attachments: r.attachments }]->m " +
				"DELETE r " +
				"WITH c, m " +
				"MATCH (m)-[i: INSIDE]->(:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]-(:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
				"SET i.trashed = true";

		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.put("userId", user.getUserId())
					.put("messageId", id)
					.put("true", true)
					.put("trash", "TRASH");
			b.add(query, params);
		}
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
	}

	@Override
	public void trashThread(List<String> threadIds, UserInfos user, Handler<Either<String, JsonObject>> result) {

	}

	@Override
	public void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;

		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->" +
				"(df:ConversationSystemFolder) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} " +
				"AND df.name = r.restoreFolder " +
				"CREATE UNIQUE df-[:HAS_CONVERSATION_MESSAGE {insideFolder: r.wasInsideFolder, attachments: r.attachments}]->m " +
				"DELETE r " +
				"WITH c, m " +
				"MATCH (m)-[i: INSIDE]->(:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]-(:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
				"REMOVE i.trashed";

		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.put("userId", user.getUserId())
					.put("messageId", id)
					.put("true", true)
					.put("trash", "TRASH");
			b.add(query, params);
		}
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
	}

	@Override
	public void delete(List<String> messagesId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if (validationError(user, result)) return;


		final String getMessage =
				"MATCH (m:ConversationMessage)<-[r:HAD_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} ";
		final String getMessageWithAttachments =
				"MATCH (a: MessageAttachment)<-[aLink:HAS_ATTACHMENT]-(m:ConversationMessage)<-[r:HAD_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} ";

		String prepareMessage =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} " +
				"OPTIONAL MATCH (m)-[i: INSIDE]->(:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]-(:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
				"CREATE f-[:HAD_CONVERSATION_MESSAGE {attachments: r.attachments}]->m " +
				"DELETE r, i ";

		String getAllAttachments =
				getMessageWithAttachments +
				"AND a.id IN r.attachments " +
				"RETURN collect({id: a.id, size: a.size}) as attachments";

		String deleteAndCollectAttachments =
				getMessageWithAttachments +
				"AND NOT(m-[:HAS_CONVERSATION_MESSAGE]-()) " +
				"DELETE aLink " +
				"WITH a, collect({id: a.id, size: a.size}) as attachments " +
				"WHERE NOT((:ConversationMessage)-[:HAS_ATTACHMENT]->(a)) " +
				"DELETE a " +
				"RETURN attachments";

		String deleteMessage =
				getMessage +
				"OPTIONAL MATCH m-[pr:PARENT_CONVERSATION_MESSAGE]-() " +
				"WITH m as message, pr " +
				"MATCH message<-[r:HAD_CONVERSATION_MESSAGE]-() " +
				"WHERE NOT(message-[:HAS_CONVERSATION_MESSAGE]-()) " +
				"DELETE r, pr, message";

		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.put("userId", user.getUserId())
					.put("messageId", id)
					.put("true", true)
					.put("trash", "TRASH");
			b.add(prepareMessage, params);
			b.add(getAllAttachments, params);
			b.add(deleteAndCollectAttachments, params);
			b.add(deleteMessage, params);
		}
		neo.executeTransaction(b.build(), null, true, validResultsHandler(result));
	}

	@Override
	public void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId)) return;

		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} " +
				"OPTIONAL MATCH (m)-[:HAS_ATTACHMENT]->(attachments: MessageAttachment) " +
				"WHERE attachments.id IN r.attachments " +
				"WITH CASE WHEN count(attachments) > 0 THEN " +
				"collect({id: attachments.id, name: attachments.name, filename: attachments.filename, contentType: attachments.contentType, " +
				"contentTransferEncoding: attachments.contentTransferEncoding, charset: attachments.charset, size: attachments.size}) " +
				"ELSE [] END as attachments, m, r, f " +
				"SET r.unread = {false} " +
				"RETURN distinct m.id as id, m.to as to, m.cc as cc, m.from as from, m.state as state, " +
				"m.subject as subject, m.date as date, m.body as body, m.toName as toName, " +
				"m.ccName as ccName, m.fromName as fromName, m.displayNames as displayNames, attachments, collect(f.name) as systemFolders";
		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("messageId", messageId)
				.put("true", true)
				.put("false", false);
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void count(String folder, String restrain, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folder)) return;
		String condition = "";
		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("folder", folder)
				.put("true", true);
		if (unread != null) {
			params.put("unread", unread);
			if (unread) {
				condition = "AND r.unread = {unread} ";
			} else {
				condition = "AND (NOT(HAS(r.unread)) OR r.unread = {unread}) ";
			}
		}
		String query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationFolder)" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folder} AND NOT HAS(r.insideFolder)" + condition +
				"RETURN count(m) as count";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void findVisibleRecipients(final String parentMessageId, final UserInfos user,
			final String acceptLanguage, String search, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		final JsonObject visible = new JsonObject();
		String replyGroupQuery;
		final JsonObject params = new JsonObject();
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			params.put("conversation", applicationName);
			replyGroupQuery =
					", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
					"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
					"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
					"AND (pg.id = visibles.id OR pg.id IN m.to OR pg.id IN m.cc) ";
			params.put("userId", user.getUserId())
					.put("parentMessageId", parentMessageId);
			String groups =
					"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
					"<-[:AUTHORIZED]-(g:Group)<-[:DEPENDS*0..1]-(pg:Group) " +
					replyGroupQuery + " AND app.name = {conversation} " +
					"RETURN DISTINCT pg.id as id, pg.name as name, pg.groupDisplayName as groupDisplayName, pg.structureName as structureName ";
			findVisibles(eb, user.getUserId(), groups, params, false, true, false,
					acceptLanguage, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibleGroups) {
					visible.put("groups", visibleGroups);
					String replyUserQuery;
						replyUserQuery =
								", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
								"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
								"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
								"AND (u.id = visibles.id OR u.id IN m.to OR u.id IN m.cc) ";
					String users =
							"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
							"<-[:AUTHORIZED]-(pg:Group)<-[:IN]-(u:User) " +
							replyUserQuery + "AND app.name = {conversation} " +
							"RETURN DISTINCT u.id as id, u.displayName as displayName, " +
							"visibles.profiles[0] as profile";
					findVisibleUsers(eb, user.getUserId(), true, true, users, params, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray visibleUsers) {
							visible.put("users", visibleUsers);
							result.handle(new Either.Right<String,JsonObject>(visible));
					   }
					});
				}
			});
		} else {
			params.put("true", true);
			String groups =
					"MATCH visibles<-[:IN*0..1]-(u:User)-[:HAS_CONVERSATION]->(c:Conversation {active:{true}}) " +
					"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
					"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName, " +
					"visibles.profiles[0] as profile, visibles.structureName as structureName";
			findVisibles(eb, user.getUserId(), groups, params, true, true, false, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibles) {
					JsonArray users = new fr.wseduc.webutils.collections.JsonArray();
					JsonArray groups = new fr.wseduc.webutils.collections.JsonArray();
					visible.put("groups", groups).put("users", users);
					for (Object o: visibles) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						if (j.getString("name") != null) {
							j.remove("displayName");
							UserUtils.groupDisplayName(j, acceptLanguage);
							groups.add(j);
						} else {
							j.remove("name");
							users.add(j);
						}
					}
					result.handle(new Either.Right<String,JsonObject>(visible));
				}
			});
		}
	}

	@Override
	public void toggleUnread(List<String> messagesId, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		// Deprecated
	}

	@Override
	public void toggleUnreadThread(List<String> threadIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {

	}

	@Override
	public void createFolder(String folderName, String parentFolderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folderName)) return;

		JsonObject newFolderProps = new JsonObject()
			.put("id", UUID.randomUUID().toString())
			.put("name", folderName);

		String completeQuery = "";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("true", true)
			.put("props", newFolderProps);

		if(parentFolderId == null){
			completeQuery = completeQuery +
				"MATCH (c:Conversation) " +
				"WHERE c.userId = {userId} AND c.active = {true} " +
				"CREATE UNIQUE (c)-[:HAS_CONVERSATION_FOLDER]->(nf: ConversationFolder:ConversationUserFolder {props}) " +
				"RETURN nf.id as id";
		} else {
			completeQuery = completeQuery +
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(pf: ConversationUserFolder) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND pf.id = {parentId} " +
				"CREATE UNIQUE (pf)-[:HAS_CHILD_FOLDER]->(nf: ConversationFolder:ConversationUserFolder {props}) "+
				"RETURN nf.id as id";

			params.put("parentId", parentFolderId);
		}

		neo.execute(completeQuery, params, validUniqueResultHandler(result));
	}

	@Override
	public void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> result) {
		final String name = data.getString("name");

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("true", true)
			.put("targetId", folderId);

		if(name != null && name.trim().length() > 0){
			params.put("newName", name);
		}

		String query =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationUserFolder)" +
			"-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(target: ConversationUserFolder) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND target.id = {targetId}" +
			"SET target.name = {newName}";

		neo.execute(query, params, validEmptyHandler(result));
	}

	@Override
	public void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result)) return;

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("true", true)
			.put("parentId", parentId);

		String query = "";
		if(parentId == null){
			query =
				"MATCH (c:Conversation)-[subLink:HAS_CONVERSATION_FOLDER]->(subFolders: ConversationUserFolder) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND NOT HAS (subLink.trashed) " +
				"RETURN DISTINCT subFolders.name as name, subFolders.id as id";
		} else {
			query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationUserFolder)" +
				"-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(target: ConversationUserFolder)"+
				"-[subLink:HAS_CHILD_FOLDER]->(subFolders: ConversationUserFolder) " +
				"WHERE target.id = {parentId} AND c.userId = {userId} AND c.active = {true} AND NOT HAS (subLink.trashed) " +
				"RETURN DISTINCT subFolders.name as name, subFolders.id as id, target.id as parentId";
		}

		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result)) return;

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("true", true);

		String query =
			"MATCH (c:Conversation)-[:TRASHED_CONVERSATION_FOLDER]->(subFolders: ConversationUserFolder) " +
			"WHERE c.userId = {userId} AND c.active = {true} " +
			"RETURN DISTINCT subFolders.name as name, subFolders.id as id";

		neo.execute(query, params, validResultHandler(result));
	}

	@Override
	public void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId)) return;

		String query =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder)-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND m.id = {messageId} " +
			"OPTIONAL MATCH (m)-[i: INSIDE]->(:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth - 1)+"]-(:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
			"DELETE i " +
			"SET r.insideFolder = true " +
			"WITH f, m " +
			"MATCH c-[HAS_CONVERSATION_FOLDER]->()-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(child: ConversationUserFolder) " +
			"WHERE child.id = {folderId} "+
			"CREATE UNIQUE m-[:INSIDE]->child";

		StatementsBuilder b = new StatementsBuilder();
		for(String id: messageIds){
			JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("messageId", id)
				.put("true", true)
				.put("folderId", folderId);

			b.add(query, params);
		}

		neo.executeTransaction(b.build(), null, true, validEmptyHandler(result));
	}

	@Override
	public void backToSystemFolder(List<String> messageIds, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result)) return;

		String query =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder)-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND m.id = {messageId} " +
			"REMOVE r.insideFolder " +
			"WITH m, c " +
			"MATCH m-[i: INSIDE]->(:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth - 1)+"]-(:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c) " +
			"DELETE i";

		StatementsBuilder b = new StatementsBuilder();
		for(String id: messageIds){
			JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("messageId", id)
				.put("true", true);

			b.add(query, params);
		}

		neo.executeTransaction(b.build(), null, true, validEmptyHandler(result));
	}

	@Override
	public void trashFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId)) return;

		//Trash actions on target folder and its subfolders
		String trashFolders =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(targetFolder: ConversationUserFolder), " +
			"(targetFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder), " +
			"(c)-[:HAS_CONVERSATION_FOLDER]->(trashFolder:ConversationSystemFolder) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND targetFolder.id = {folderId} AND trashFolder.name = {trash} " +
			"WITH c, targetFolder, trashFolder " +
			"CREATE UNIQUE (c)-[:TRASHED_CONVERSATION_FOLDER]->(targetFolder) " +
			"WITH targetFolder " +
			"OPTIONAL MATCH (targetParent: ConversationUserFolder)-[targetParentRel: HAS_CHILD_FOLDER]->(targetFolder) " +
			"OPTIONAL MATCH (systemParent: Conversation)-[systemParentRel: HAS_CONVERSATION_FOLDER]->(targetFolder) " +
			"SET targetParentRel.trashed = true, systemParentRel.trashed = true";

		//Trash actions on messages contained inside the folder and its children
		String trashMessages =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(targetFolder: ConversationUserFolder), " +
			"(targetFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder), " +
			"(c)-[:HAS_CONVERSATION_FOLDER]->(trashFolder:ConversationSystemFolder), " +
			"(c)-[:HAS_CONVERSATION_FOLDER]->(f: ConversationSystemFolder)-[r:HAS_CONVERSATION_MESSAGE]->(messages: ConversationMessage)-[i: INSIDE]->(children) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND targetFolder.id = {folderId} AND trashFolder.name = {trash} AND NOT HAS(i.trashed) " +
			"CREATE UNIQUE (trashFolder)-[:HAS_CONVERSATION_MESSAGE { restoreFolder: f.name, insideFolder: true, attachments: r.attachments }]->(messages) " +
			"DELETE r " +
			"SET i.trashed = true";

		//Trash actions on children folders already put in the bin before
		String alreadyTrashedFolders =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(targetFolder: ConversationUserFolder), " +
			"(targetFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder), " +
			"(childrenParents: ConversationFolder)-[childrenParentRel {trashed: true}]->(children)<-[childrenTrashedRel: TRASHED_CONVERSATION_FOLDER]-(c) " +
			"WHERE targetFolder.id = {folderId} " +
			"REMOVE childrenParentRel.trashed " +
			"DELETE childrenTrashedRel";

		//Trash actions on children messages already put in the bin before
		String alreadyTrashedMails =
			"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(targetFolder: ConversationUserFolder), " +
			"(targetFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder), " +
			"(trashFolder)-[trashRel: HAS_CONVERSATION_MESSAGE]->(:ConversationMessage)-[:INSIDE {trashed: true}]->(children) " +
			"WHERE targetFolder.id = {folderId} " +
			"SET trashRel.insideFolder = true";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("folderId", folderId)
			.put("true", true)
			.put("trash", "TRASH");

		StatementsBuilder b = new StatementsBuilder();
		b.add(alreadyTrashedFolders, params);
		b.add(alreadyTrashedMails, params);
		b.add(trashMessages, params);
		b.add(trashFolders, params);
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
	}

	@Override
	public void restoreFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, folderId)) return;

		String query =
			"MATCH (c: Conversation)-[trashedRel: TRASHED_CONVERSATION_FOLDER]->(trashedFolder: ConversationUserFolder)" +
			"<-[targetParentRel: HAS_CHILD_FOLDER|HAS_CONVERSATION_FOLDER { trashed: true }]-(targetParent) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashedFolder.id = {folderId} " +
			"DELETE trashedRel " +
			"REMOVE targetParentRel.trashed "+
			"WITH c, trashedFolder " +
			"MATCH (c)-[:HAS_CONVERSATION_FOLDER]->(trashFolder: ConversationSystemFolder) " +
			"WHERE trashFolder.name = {trash} " +
			"MATCH (trashedFolder)-[:HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder)<-[i: INSIDE]-(messages: ConversationMessage), " +
			"(trashFolder)-[r:HAS_CONVERSATION_MESSAGE]->(messages), " +
			"(c)-[:HAS_CONVERSATION_FOLDER]->(oldFolder: ConversationSystemFolder) " +
			"WHERE oldFolder.name = r.restoreFolder " +
			"CREATE UNIQUE (oldFolder)-[:HAS_CONVERSATION_MESSAGE { insideFolder: true, attachments: r.attachments }]->(messages) " +
			"DELETE r " +
			"REMOVE i.trashed";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("folderId", folderId)
			.put("true", true)
			.put("trash", "TRASH");

		neo.execute(query, params, validEmptyHandler(result));
	}

	@Override
	public void deleteFolder(String folderId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result) {
		if(validationError(user, result, folderId)) return;

		String retrieveAttachments =
			"MATCH (c: Conversation)-[:TRASHED_CONVERSATION_FOLDER]->(trashedFolder:ConversationUserFolder) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashedFolder.id = {folderId} " +
			"MATCH (c)-[:HAS_CONVERSATION_FOLDER]->(trashFolder: ConversationSystemFolder) " +
			"WHERE trashFolder.name = {trash} " +
			"MATCH (trashedFolder)-[childrenPath: HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder) " +
			"MATCH (children)<-[i: INSIDE]-(messages: ConversationMessage)<-[r: HAS_CONVERSATION_MESSAGE]-(trashFolder) " +
			"MATCH (a: MessageAttachment)<-[:HAS_ATTACHMENT]-(messages) " +
			"WHERE a.id IN r.attachments " +
			"WITH CASE WHEN a IS NULL THEN [] ELSE collect({id: a.id, size: a.size}) END as attachments " +
			"RETURN attachments";

		String processMessages =
			"MATCH (c: Conversation)-[trashedRel: TRASHED_CONVERSATION_FOLDER]->(trashedFolder: ConversationUserFolder)" +
			"<-[targetParentRel: HAS_CHILD_FOLDER|HAS_CONVERSATION_FOLDER { trashed: true }]-(targetParent) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashedFolder.id = {folderId} " +
			"MATCH (c)-[:HAS_CONVERSATION_FOLDER]->(trashFolder: ConversationSystemFolder) " +
			"WHERE trashFolder.name = {trash} " +
			"MATCH (trashedFolder)-[childrenPath: HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder) " +
			"MATCH (children)<-[i: INSIDE]-(messages: ConversationMessage)<-[r: HAS_CONVERSATION_MESSAGE]-(trashFolder) " +
			"OPTIONAL MATCH (messages)-[pr: PARENT_CONVERSATION_MESSAGE]-() " +
			"CREATE (trashFolder)-[:HAD_CONVERSATION_MESSAGE { attachments: r.attachments }]->(messages) " +
			"DELETE r, i";

		String gatherAttachmentsToDelete =
			"MATCH (c: Conversation)-[:HAS_CONVERSATION_FOLDER]->(trashFolder: ConversationSystemFolder)-[r:HAD_CONVERSATION_MESSAGE]->(messages: ConversationMessage) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashFolder.name = {trash} " +
			"AND NOT (messages)<-[:HAS_CONVERSATION_MESSAGE]-() " +
			"MATCH (messages)-[al: HAS_ATTACHMENT]->(a: MessageAttachment) " +
			"MATCH (a)<-[aLinks:HAS_ATTACHMENT]-(target) " +
			"WITH a, count(target) as links " +
			"WHERE links = 1 " +
			"MATCH (a)<-[l:HAS_ATTACHMENT]-(target) " +
			"WITH collect({id: a.id, size: a.size}) as attachments, l, a " +
			"DELETE l, a " +
			"RETURN attachments";

		String deleteMessages =
			"MATCH (c: Conversation)-[:HAS_CONVERSATION_FOLDER]->(trashFolder: ConversationSystemFolder)-[:HAD_CONVERSATION_MESSAGE]->(messages: ConversationMessage) " +
			"MATCH (messages)-[r:HAD_CONVERSATION_MESSAGE]-() " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashFolder.name = {trash} " +
			"AND NOT (messages)-[:HAS_CONVERSATION_MESSAGE]-() " +
			"OPTIONAL MATCH (messages)-[pr: PARENT_CONVERSATION_MESSAGE]-() " +
			"OPTIONAL MATCH (messages)-[al: HAS_ATTACHMENT]->(a: MessageAttachment) " +
			"DELETE r, pr, al, messages";

		String deleteFolders =
			"MATCH (c: Conversation)-[trashedRel: TRASHED_CONVERSATION_FOLDER]->(trashedFolder: ConversationUserFolder)" +
			"<-[targetParentRel: HAS_CHILD_FOLDER|HAS_CONVERSATION_FOLDER { trashed: true }]-(targetParent) " +
			"WHERE c.userId = {userId} AND c.active = {true} AND trashedFolder.id = {folderId} " +
			"MATCH (trashedFolder)-[childrenPath: HAS_CHILD_FOLDER*0.."+(maxFolderDepth-1)+"]->(children: ConversationUserFolder) " +
			"FOREACH (rel in childrenPath | DELETE rel) " +
			"DELETE targetParentRel, trashedRel, trashedFolder, children";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("folderId", folderId)
			.put("true", true)
			.put("trash", "TRASH");

		StatementsBuilder b = new StatementsBuilder();
		b.add(retrieveAttachments, params);
		b.add(processMessages, params);
		b.add(gatherAttachmentsToDelete, params);
		b.add(deleteMessages, params);
		b.add(deleteFolders, params);
		neo.executeTransaction(b.build(), null, true, validResultsHandler(result));
	}

	@Override
	public void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId)) return;

		String query =
			"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
			"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
			"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND m.state = {draft} " +
			"CREATE (m)-[:HAS_ATTACHMENT]->(attachment: MessageAttachment {attachmentProps}) " +
			"SET r.attachments = coalesce(r.attachments, []) + {fileId} " +
			"RETURN attachment.id as id";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("messageId", messageId)
			.put("attachmentProps", new JsonObject()
				.put("id", uploaded.getString("_id"))
				.put("name", uploaded.getJsonObject("metadata").getString("name"))
				.put("filename", uploaded.getJsonObject("metadata").getString("filename"))
				.put("contentType", uploaded.getJsonObject("metadata").getString("content-type"))
				.put("contentTransferEncoding", uploaded.getJsonObject("metadata").getString("content-transfer-encoding"))
				.put("charset", uploaded.getJsonObject("metadata").getString("charset"))
				.put("size", uploaded.getJsonObject("metadata").getLong("size")))
			.put("fileId", uploaded.getString("_id"))
			.put("true", true)
			.put("trash", "TRASH")
			.put("draft", "DRAFT");

		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId)) return;

		String query =
			"MATCH (attachment: MessageAttachment)<-[attachmentLink: HAS_ATTACHMENT]-(m: ConversationMessage)<-[r: HAS_CONVERSATION_MESSAGE]-(f: ConversationSystemFolder)" +
			"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
			"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND attachment.id = {attachmentId} AND {attachmentId} IN r.attachments " +
			"WITH distinct attachment " +
			"RETURN attachment.id as id, attachment.name as name, attachment.filename as filename, attachment.contentType as contentType, attachment.contentTransferEncoding as contentTransferEncoding, attachment.charset as charset, attachment.size as size";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("messageId", messageId)
			.put("attachmentId", attachmentId)
			.put("true", true);

		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result) {
		result.handle(new Either.Left<String, JsonArray>("conversation.invalid"));
	}

	@Override
	public void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result) {
		if(validationParamsError(user, result, messageId, attachmentId)) return;

		String get =
			"MATCH (attachment: MessageAttachment)<-[aLink: HAS_ATTACHMENT]-(m: ConversationMessage)<-[r: HAS_CONVERSATION_MESSAGE]-(f: ConversationSystemFolder)" +
			"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
			"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND attachment.id = {attachmentId} AND {attachmentId} IN r.attachments ";

		String query =
			get +
			"SET r.attachments = filter(attachmentId IN r.attachments WHERE attachmentId <> {attachmentId}) " +
			"RETURN attachment.id as fileId, attachment.size as fileSize";

		String q3 =
			"MATCH (attachment:MessageAttachment)<-[attachmentLink: HAS_ATTACHMENT]-(:ConversationMessage)<-[messageLinks: HAS_CONVERSATION_MESSAGE]-(:ConversationSystemFolder) " +
			"WHERE attachment.id = {attachmentId} " +
			"WITH attachmentLink, attachment, none(item IN collect(messageLinks.attachments) WHERE attachment.id IN item) as deletionCheck " +
			"WHERE deletionCheck = true " +
			"DELETE attachmentLink " +
			"WITH attachment " +
			"WHERE NOT(attachment-[:HAS_ATTACHMENT]-()) " +
			"DELETE attachment " +
			"RETURN true as deletionCheck";

		JsonObject params = new JsonObject()
			.put("userId", user.getUserId())
			.put("messageId", messageId)
			.put("attachmentId", attachmentId)
			.put("true", true);

		StatementsBuilder b = new StatementsBuilder();
		b.add(query, params);
		b.add(q3, params);

		neo.executeTransaction(b.build(), null, true, validResultsHandler(new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if(event.isLeft()){
					result.handle(new Either.Left<String, JsonObject>(event.left().getValue()));
					return;
				}

				JsonArray result1 = event.right().getValue().getJsonArray(0);
				JsonArray result3 = event.right().getValue().getJsonArray(1);

				JsonObject jsonResult = result1.size() > 0 ?
						result1.getJsonObject(0) :
						new JsonObject();
				jsonResult.put("deletionCheck", result3.size() > 0 ?
							result3.getJsonObject(0).getBoolean("deletionCheck", false) :
							false);

				result.handle(new Either.Right<String, JsonObject>(jsonResult));
			}
		}));
	}

	@Override
	public void forwardAttachments(String forwardId, String messageId, UserInfos user,
			Handler<Either<String, JsonObject>> result){
		if (validationParamsError(user, result, messageId)) return;

		JsonObject params = new JsonObject()
				.put("userId", user.getUserId())
				.put("folderName", "DRAFT")
				.put("forwardId", forwardId)
				.put("messageId", messageId)
				.put("true", true);

		String query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder)" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage {id: {messageId}}), " +
				"(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(af:ConversationSystemFolder)" +
				"-[r2:HAS_CONVERSATION_MESSAGE]->(pm:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folderName} " +
				"AND pm.id = {forwardId} " +
				"OPTIONAL MATCH (pm)-[:HAS_ATTACHMENT]->(a: MessageAttachment) " +
				"SET r.attachments = r2.attachments " +
				"WITH distinct m, a " +
				"FOREACH (attachment IN CASE WHEN a IS NOT NULL THEN [a] ELSE [] END | " +
				"CREATE (m)-[:HAS_ATTACHMENT]->(attachment) )";

		neo.execute(query, params, validEmptyHandler(result));

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

	private String nodeSetPropertiesFromJson(String nodeAlias, JsonObject json) {
		StringBuilder sb = new StringBuilder();
		for (String attr: json.fieldNames()) {
			sb.append(", ").append(nodeAlias).append(".").append(attr).append(" = {").append(attr).append("}");
		}
		if (sb.length() > 2) {
			return sb.append(" ").substring(2);
		}
		return " ";
	}

}
