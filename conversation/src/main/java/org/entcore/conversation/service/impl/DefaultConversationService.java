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

import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.user.UserUtils.findVisibleUsers;
import static org.entcore.common.user.UserUtils.findVisibles;

import fr.wseduc.webutils.Server;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.conversation.service.ConversationService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.*;

public class DefaultConversationService implements ConversationService {

	private static final int LIST_LIMIT = 25;
	private final EventBus eb;
	private final Neo neo;
	private final String applicationName;

	public DefaultConversationService(Vertx vertx, String applicationName) {
		eb = Server.getEventBus(vertx);
		neo = new Neo(eb, LoggerFactory.getLogger(Neo.class));
		this.applicationName = applicationName;
	}

	@Override
	public void saveDraft(final String parentMessageId, final JsonObject message,
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
		ids.addAll(message.getArray("to", new JsonArray()).toList());
		ids.addAll(message.getArray("cc", new JsonArray()).toList());
		if (message.containsField("from")) {
			ids.add(message.getString("from"));
		}
		neo.execute(query, new JsonObject().putArray("ids", new JsonArray(ids.toArray())),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				JsonArray r = m.body().getArray("result");
				if ("ok".equals(m.body().getString("status")) && r != null && r.size() == 1) {
					JsonObject j = r.get(0);
					JsonArray d = j.getArray("displayNames");
					if (d != null && d.size() > 0) {
						message.putArray("displayNames", d);
					}
				}
				handler.handle(message);
			}
		});
	}

	private boolean displayNamesCondition(JsonObject message) {
		return message != null && (
				(message.containsField("from") && !message.getString("from").trim().isEmpty()) ||
				(message.containsField("to") && message.getArray("to").size() > 0) ||
				(message.containsField("cc") && message.getArray("cc").size() > 0));
	}

	private void save(String parentMessageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result) {
		if (message == null) {
			message = new JsonObject();
		}
		message.putString("id", UUID.randomUUID().toString())
				.putNumber("date", System.currentTimeMillis())
				.putString("from", user.getUserId())
				.putString("state", State.DRAFT.name());
		JsonObject m = Utils.validAndGet(message, MESSAGE_FIELDS, DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result)) return;
		String query;
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("folderName", "DRAFT")
				.putObject("props", m)
				.putBoolean("true", true);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) { // reply
			query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder), " +
				"(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(af:ConversationSystemFolder)" +
				"-[:HAS_CONVERSATION_MESSAGE]->(pm:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folderName} " +
				"AND pm.id = {parentMessageId} " +
				"WITH f, pm, count(f) as nb " +
				"WHERE nb = 1 " +
				"CREATE f-[:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage {props})" +
				"-[:PARENT_CONVERSATION_MESSAGE]->pm " +
				"RETURN m.id as id";
			params.putString("parentMessageId", parentMessageId);
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
		message.putNumber("date", System.currentTimeMillis());
		JsonObject m = Utils.validAndGet(message, UPDATE_DRAFT_FIELDS, UPDATE_DRAFT_REQUIRED_FIELDS);
		if (validationError(user, m, result, messageId)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
				"<-[r:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {id} AND m.from = {userId} AND m.state = {state} AND c.active = {true} " +
				"SET " + nodeSetPropertiesFromJson("m", m) +
				"RETURN m.id as id";
		m.putString("userId", user.getUserId())
				.putString("id", messageId)
				.putString("state", State.DRAFT.name())
				.putBoolean("true", true);
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

	private void send(final String parentMessageId, String messageId, UserInfos user,
			final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId)) return;
		String usersQuery;
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("messageId", messageId)
				.putString("draft", State.DRAFT.name())
				.putString("outbox", "OUTBOX")
				.putString("inbox", "INBOX")
				.putString("sent", State.SENT.name())
				.putBoolean("true", true);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) { // reply
			usersQuery =
					"MATCH (m:ConversationMessage { id : {parentMessageId}}) " +
					"WITH (COLLECT(visibles.id) + m.to + m.cc + m.from) as vis " +
					"MATCH (v:Visible) " +
					"WHERE v.id IN vis " +
					"WITH DISTINCT v ";
			params.putString("parentMessageId", parentMessageId);
		} else {
			usersQuery = "WITH visibles as v ";
		}
		String query =
				usersQuery +
				"MATCH v<-[:IN*0..1]-(u:User), (message:ConversationMessage) " +
				"WHERE (v: User or v:ProfileGroup) " +
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
				"CREATE UNIQUE fOut-[:HAS_CONVERSATION_MESSAGE]->message " +
				"DELETE r " +
				"RETURN EXTRACT(u IN FIlTER(x IN users WHERE NOT(x.id IN sentIds)) | u.displayName) as undelivered,  " +
				"EXTRACT(u IN FIlTER(x IN users WHERE x.id IN sentIds AND NOT(x.activationCode IS NULL)) " +
				"| u.displayName) as inactive, LENGTH(sentIds) as sent, " +
				"sentIds, message.id as id, message.subject as subject";
		findVisibles(eb, user.getUserId(), query, params, true, true, false, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray event) {
				if (event != null && event.size() == 1 && (event.get(0) instanceof JsonObject)) {
					result.handle(new Either.Right<String, JsonObject>((JsonObject) event.get(0)));
				} else {
					result.handle(new Either.Left<String, JsonObject>("conversation.send.error"));
				}
			}
		});
	}

	@Override
	public void list(String folder, UserInfos user, int page, final Handler<Either<String, JsonArray>> results) {
		if (validationError(user, results, folder)) return;
		int skip = page * LIST_LIMIT;
		String query =
				"MATCH (c:Conversation {userId : {userId}, active : {true}})-[:HAS_CONVERSATION_FOLDER]->" +
				"(f:ConversationFolder {name : {folder}})" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
				"RETURN m.id as id, m.to as to, m.from as from, m.state as state, " +
				"m.toName as toName, m.fromName as fromName, " +
				"m.subject as subject, m.date as date, r.unread as unread, m.displayNames as displayNames " +
				"ORDER BY m.date DESC " +
				"SKIP {skip} " +
				"LIMIT {limit} ";
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("folder", folder)
				.putNumber("skip", skip)
				.putNumber("limit", LIST_LIMIT)
				.putBoolean("true", true);
		neo.execute(query, params, validResultHandler(results));
	}

	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->" +
				"(df:ConversationSystemFolder) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND df.name = {trash} " +
				"CREATE UNIQUE df-[:HAS_CONVERSATION_MESSAGE { restoreFolder: f.name }]->m " +
				"DELETE r ";
		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.putString("userId", user.getUserId())
					.putString("messageId", id)
					.putBoolean("true", true)
					.putString("trash", "TRASH");
			b.add(query, params);
		}
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
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
				"CREATE UNIQUE df-[:HAS_CONVERSATION_MESSAGE]->m " +
				"DELETE r ";
		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.putString("userId", user.getUserId())
					.putString("messageId", id)
					.putBoolean("true", true)
					.putString("trash", "TRASH");
			b.add(query, params);
		}
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
	}

	@Override
	public void delete(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} " +
				"OPTIONAL MATCH m-[pr:PARENT_CONVERSATION_MESSAGE]-() " +
				"CREATE f-[:HAD_CONVERSATION_MESSAGE]->m " +
				"DELETE r " +
				"WITH m as message, pr " +
				"MATCH message<-[r:HAD_CONVERSATION_MESSAGE]-() " +
				"WHERE NOT(message-[:HAS_CONVERSATION_MESSAGE]-()) " +
				"DELETE r, pr, message";
		StatementsBuilder b = new StatementsBuilder();
		for (String id: messagesId) {
			JsonObject params = new JsonObject()
					.putString("userId", user.getUserId())
					.putString("messageId", id)
					.putBoolean("true", true)
					.putString("trash", "TRASH");
			b.add(query, params);
		}
		neo.executeTransaction(b.build(), null, true, validUniqueResultHandler(result));
	}

	@Override
	public void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, messageId)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-f" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} " +
				"SET r.unread = {false} " +
				"RETURN distinct m.id as id, m.to as to, m.cc as cc, m.from as from, m.state as state, " +
				"m.subject as subject, m.date as date, m.body as body, m.toName as toName, " +
				"m.ccName as ccName, m.fromName as fromName, m.displayNames as displayNames ";
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("messageId", messageId)
				.putBoolean("true", true)
				.putBoolean("false", false);
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void count(String folder, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result, folder)) return;
		String condition = "";
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("folder", folder)
				.putBoolean("true", true);
		if (unread != null) {
			params.putBoolean("unread", unread);
			if (unread) {
				condition = "AND r.unread = {unread} ";
			} else {
				condition = "AND (NOT(HAS(r.unread)) OR r.unread = {unread}) ";
			}
		}
		String query =
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationFolder)" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folder} " + condition +
				"RETURN count(m) as count";
		neo.execute(query, params, validUniqueResultHandler(result));
	}

	@Override
	public void findVisibleRecipients(final String parentMessageId, final UserInfos user,
			final String acceptLanguage, final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		final JsonObject visible = new JsonObject();
		String replyProfileGroupQuery;
		final JsonObject params = new JsonObject();
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			params.putString("conversation", applicationName);
			replyProfileGroupQuery =
					", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
					"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
					"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
					"AND (pg.id = visibles.id OR pg.id IN m.to OR pg.id IN m.cc) ";
			params.putString("userId", user.getUserId())
					.putString("parentMessageId", parentMessageId);
			String groups =
					"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
					"<-[:AUTHORIZED]-(g:ProfileGroup)<-[:DEPENDS*0..1]-(pg:ProfileGroup) " +
					replyProfileGroupQuery + " AND app.name = {conversation} " +
					"RETURN DISTINCT pg.id as id, pg.name as name, pg.groupDisplayName as groupDisplayName";
			findVisibles(eb, user.getUserId(), groups, params, false, true, false,
					acceptLanguage, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibleGroups) {
					visible.putArray("groups", visibleGroups);
					String replyUserQuery;
						replyUserQuery =
								", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
								"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
								"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
								"AND (u.id = visibles.id OR u.id IN m.to OR u.id IN m.cc) ";
					String users =
							"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
							"<-[:AUTHORIZED]-(pg:ProfileGroup)<-[:IN]-(u:User) " +
							replyUserQuery + "AND app.name = {conversation} " +
							"RETURN DISTINCT u.id as id, u.displayName as displayName";
					findVisibleUsers(eb, user.getUserId(), true, false, users, params, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray visibleUsers) {
							visible.putArray("users", visibleUsers);
							result.handle(new Either.Right<String,JsonObject>(visible));
					   }
					});
				}
			});
		} else {
			params.putBoolean("true", true);
			String groups =
					"MATCH visibles<-[:IN*0..1]-(u:User)-[:HAS_CONVERSATION]->(c:Conversation {active:{true}}) " +
					"RETURN DISTINCT visibles.id as id, visibles.name as name, " +
					"visibles.displayName as displayName, visibles.groupDisplayName as groupDisplayName";
			findVisibles(eb, user.getUserId(), groups, params, true, true, false, new Handler<JsonArray>() {
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
		for (String attr: json.getFieldNames()) {
			sb.append(", ").append(nodeAlias).append(".").append(attr).append(" = {").append(attr).append("}");
		}
		if (sb.length() > 2) {
			return sb.append(" ").substring(2);
		}
		return " ";
	}

}
