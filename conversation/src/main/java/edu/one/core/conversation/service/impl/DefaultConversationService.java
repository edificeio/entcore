package edu.one.core.conversation.service.impl;

import static edu.one.core.common.neo4j.Neo4jResult.*;
import static edu.one.core.common.user.UserUtils.findVisibleUsers;
import static edu.one.core.common.user.UserUtils.findVisibles;

import com.google.common.base.Joiner;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.common.neo4j.StatementsBuilder;
import edu.one.core.common.user.UserInfos;
import edu.one.core.conversation.service.ConversationService;
import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.*;

public class DefaultConversationService implements ConversationService {

	private static final int LIST_LIMIT = 50;
	private final EventBus eb;
	private final Neo neo;
	private final String applicationName;

	public DefaultConversationService(Vertx vertx, String applicationName) {
		eb = vertx.eventBus();
		neo = new Neo(eb, LoggerFactory.getLogger(Neo.class));
		this.applicationName = applicationName;
	}

	@Override
	public void saveDraft(String parentMessageId, JsonObject message, UserInfos user, Handler<Either<String, JsonObject>> result) {
		save(parentMessageId, message, user, result);
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
	public void updateDraft(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result) {
		update(messageId, message, user, result);
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
					"WITH COLLECT(visibles.id) as vis " +
					"MATCH (m:ConversationMessage), (v:Visible) " +
					"WHERE m.id = {parentMessageId} AND " +
					"(v.id IN vis OR v.id = m.from OR v.id IN m.to OR v.id IN m.cc) " +
					"WITH DISTINCT v ";
			params.putString("parentMessageId", parentMessageId);
		} else {
			usersQuery = "WITH visibles as v ";
		}
		String query =
				usersQuery +
				"MATCH v<-[:APPARTIENT*0..1]-(u:User), " +
				"(message:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(fDraft:ConversationSystemFolder), " +
				"(sender:Conversation)-[:HAS_CONVERSATION_FOLDER]->(fOut:ConversationSystemFolder) " +
				"WHERE (v:User OR v:ProfileGroup) AND u.id <> {userId} " +
				"AND message.id = {messageId} AND message.state = {draft} AND message.from = {userId} AND " +
				"fDraft.name = {draft} AND sender.userId = {userId} AND " +
				"(v.id IN message.to OR v.id IN message.cc) AND fOut.name = {outbox} " +
				"CREATE UNIQUE fOut-[:HAS_CONVERSATION_MESSAGE]->message " +
				"DELETE r " +
				"WITH u, message " +
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationSystemFolder) " +
				"WHERE c.userId = u.id AND c.active = {true} AND f.name = {inbox} " +
				"CREATE UNIQUE f-[:HAS_CONVERSATION_MESSAGE { unread: {true} }]->message " +
				"WITH COLLECT(c.userId) as sentIds, COLLECT(u) as users, message " +
				"SET message.state = {sent} " +
				"RETURN EXTRACT(u IN FIlTER(x IN users WHERE NOT(x.id IN sentIds)) | u.displayName) as undelivered,  " +
				"EXTRACT(u IN FIlTER(x IN users WHERE x.id IN sentIds AND NOT(x.activationCode IS NULL)) " +
				"| u.displayName) as inactive, LENGTH(sentIds) as sent, " +
				"sentIds, message.id as id, message.subject as subject";
		findVisibles(eb, user.getUserId(), query, params, false, true, new Handler<JsonArray>() {
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
				"MATCH (c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(f:ConversationFolder)" +
				"-[r:HAS_CONVERSATION_MESSAGE]->(m:ConversationMessage) " +
				"WHERE c.userId = {userId} AND c.active = {true} AND f.name = {folder} " +
				"RETURN m.id as id, m.to as to, m.from as from, m.state as state, " +
				"m.subject as subject, m.date as date, r.unread as unread " +
				"ORDER BY date DESC " +
				"SKIP {skip} " +
				"LIMIT {limit} ";
		JsonObject params = new JsonObject()
				.putString("userId", user.getUserId())
				.putString("folder", folder)
				.putNumber("skip", skip)
				.putNumber("limit", LIST_LIMIT)
				.putBoolean("true", true);
		neo.execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() > 0) {
					Set<Object> ugids = new HashSet<>();
					List<String> ids = new ArrayList<>();
					for (Object o : res) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						ids.add(j.getString("id"));
						ugids.addAll(Arrays.<Object>asList(j.getArray("to", new JsonArray()).toArray()));
						ugids.add(j.getString("from"));
					}
					String query2 =
							"MATCH (dn:Visible) " +
							"WHERE dn.id IN ['" + Joiner.on("','").join(ugids) + "'] " +
							"WITH dn " +
							"MATCH (m:ConversationMessage) " +
							"WHERE m.id IN ['" + Joiner.on("','").join(ids) + "'] " +
							"AND (dn.id = m.from OR dn.id IN m.to ) " +
							"RETURN m.id, " +
							"COLLECT([dn.id, CASE WHEN dn.displayName IS NULL THEN dn.name ELSE dn.displayName END]) " +
							"as displayNames, m.date as date " +
							"ORDER BY date DESC ";
					neo.execute(query2, new JsonObject(), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							JsonArray res2 = event.body().getArray("result");
							if ("ok".equals(event.body().getString("status")) && res2 != null
									&& res2.size() > 0 && res.size() == res2.size()) {
								for (int i = 0; i < res.size(); i++) {
									JsonObject j = res.get(i);
									JsonObject j2 = res2.get(i);
									j.putArray("displayNames", j2.getArray("displayNames"));
								}
								results.handle(new Either.Right<String, JsonArray>(res));
							} else {
								results.handle(validResults(event));
							}
						}
					});
				} else {
					results.handle(validResult(event));
				}
			}
		});
	}

	@Override
	public void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		String query =
				"MATCH (m:ConversationMessage)<-[r:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)" +
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation), " +
				"(c2)-[:HAS_CONVERSATION_FOLDER]->(df:ConversationSystemFolder) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND df.name = {trash} " +
				"AND c2.userId = {userId} AND c2.active = {true}" +
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
				"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation), " +
				"(c2)-[:HAS_CONVERSATION_FOLDER]->(df:ConversationSystemFolder) " +
				"WHERE m.id = {messageId} AND c.userId = {userId} AND c.active = {true} AND f.name = {trash} " +
				"AND c2.userId = {userId} AND c2.active = {true} AND df.name = r.restoreFolder " +
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
				"OPTIONAL MATCH message-[pr:PARENT_CONVERSATION_MESSAGE]-() " +
				"DELETE r " +
				"WITH m as message, pr " +
				"WHERE NOT(message-[:HAS_CONVERSATION_MESSAGE]-()) " +
				"DELETE pr, message";
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
				"WITH distinct m.id as id, m.to as to, m.cc as cc, m.from as from, m.state as state, " +
				"m.subject as subject, m.date as date, m.body as body " +
				"MATCH (dn:Visible) " +
				"WHERE dn.id = from OR dn.id IN to OR dn.id IN cc " +
				"RETURN id, to, cc, from, state, subject, date, body, " +
				"COLLECT([dn.id, CASE WHEN dn.displayName IS NULL THEN dn.name ELSE dn.displayName END]) " +
				"as displayNames";
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
			final Handler<Either<String, JsonObject>> result) {
		if (validationParamsError(user, result)) return;
		final JsonObject visible = new JsonObject();
		String replyProfileGroupQuery;
		final JsonObject params = new JsonObject()
				.putString("conversation", applicationName);
		if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
			replyProfileGroupQuery =
					", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
					"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
					"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
					"AND (pg.id = visibles.id OR pg.id IN m.to OR pg.id IN m.cc) ";
			params.putString("userId", user.getUserId())
					.putString("parentMessageId", parentMessageId);
		} else {
			replyProfileGroupQuery = " WHERE pg.id = visibles.id ";
		}
		String groups =
				"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
				"<-[:AUTHORIZED]-(g:ProfileGroup)<-[:DEPENDS*0..1]-(pg:ProfileGroup) " +
				replyProfileGroupQuery + " AND app.name = {conversation} " +
				"RETURN DISTINCT pg.id as id, pg.name as name";
		findVisibles(eb, user.getUserId(), groups, params, false, true, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray visibleGroups) {
				visible.putArray("groups", visibleGroups);
				String replyUserQuery;
				if (parentMessageId != null && !parentMessageId.trim().isEmpty()) {
					replyUserQuery =
							", (m:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-f" +
							"<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) " +
							"WHERE m.id = {parentMessageId} AND c.userId = {userId} " +
							"AND (u.id = visibles.id OR u.id IN m.to OR u.id IN m.cc) ";
				} else {
					replyUserQuery = " WHERE u.id = visibles.id ";
				}
				String users =
						"MATCH (app:Application)-[:PROVIDE]->(a:Action)<-[:AUTHORIZE]-(r:Role)" +
						"<-[:AUTHORIZED]-(pg:ProfileGroup)<-[:APPARTIENT]-(u:User) " +
						replyUserQuery + "AND app.name = {conversation} " +
						"RETURN DISTINCT u.id as id, u.displayName as displayName";
				findVisibleUsers(eb, user.getUserId(), false, users, params, new Handler<JsonArray>() {
					@Override
					public void handle(JsonArray visibleUsers) {
						visible.putArray("users", visibleUsers);
						result.handle(new Either.Right<String,JsonObject>(visible));
					}
				});
			}
		});
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
