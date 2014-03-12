package org.entcore.conversation.service.impl;

import fr.wseduc.webutils.collections.Joiner;
import org.entcore.common.appregistry.AppRegistryEventsService;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ConversationServiceManager implements AppRegistryEventsService {

	private final EventBus eb;
	private final Neo neo;
	private final String applicationName;

	public ConversationServiceManager(Vertx vertx, String applicationName) {
		eb = vertx.eventBus();
		neo = new Neo(eb, LoggerFactory.getLogger(Neo.class));
		this.applicationName = applicationName;
	}

	@Override
	public void authorizedActionsUpdated(final JsonArray groups) {
		ApplicationUtils.applicationAllowedUsers(eb, applicationName, null, groups, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				final Set<String> userIds = new HashSet<>();
				for (Object o: users) {
					if (!(o instanceof JsonObject)) continue;
					userIds.add(((JsonObject) o).getString("id"));
				}
				if (groups != null) {
					usersInGroups(groups, new Handler<JsonArray>(){

						@Override
						public void handle(JsonArray uig) {
							manageConversationNodes(userIds, uig);
						}
					});
				} else {
					manageConversationNodes(userIds, null);
				}
			}
		});
	}

	@Override
	public void userGroupUpdated(final JsonArray users, final Message<JsonObject> message) {
		ApplicationUtils.applicationAllowedUsers(eb, applicationName, users, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray u) {
				Set<String> userIds = new HashSet<>();
				for (Object o: u) {
					if (!(o instanceof JsonObject)) continue;
					userIds.add(((JsonObject) o).getString("id"));
				}
				manageConversationNodes(userIds, users, message);
			}
		});
	}

	private void manageConversationNodes(Set<String> userIds, JsonArray modifiedUsers) {
		manageConversationNodes(userIds, modifiedUsers, null);
	}

	private void manageConversationNodes(Set<String> userIds, JsonArray modifiedUsers,
			final Message<JsonObject> message) {
		String filter = "";
		JsonObject disableParams = new JsonObject().putBoolean("false", false);
		if (modifiedUsers != null) {
			filter = "AND c.userId IN {modifiedUsers} ";
			disableParams.putArray("modifiedUsers", modifiedUsers);
		}
		StatementsBuilder b = new StatementsBuilder().add(
				"MATCH (u:User) " +
				"WHERE u.id IN ['" + Joiner.on("','").join(userIds) + "'] " +
				"AND NOT(u-[:HAS_CONVERSATION]->()) " +
				"CREATE UNIQUE u-[:HAS_CONVERSATION]->(c:Conversation { userId : u.id, active : {true} }), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fi:ConversationFolder:ConversationSystemFolder { name : {inbox}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fo:ConversationFolder:ConversationSystemFolder { name : {outbox}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(fd:ConversationFolder:ConversationSystemFolder { name : {draft}}), " +
				"c-[:HAS_CONVERSATION_FOLDER]->(ft:ConversationFolder:ConversationSystemFolder { name : {trash}}) ",
				new JsonObject().putString("inbox", "INBOX").putString("outbox", "OUTBOX")
						.putString("draft", "DRAFT").putString("trash", "TRASH").putBoolean("true", true))
				.add(
				"MATCH (c:Conversation) " +
				"WHERE c.userId IN ['" + Joiner.on("','").join(userIds) + "'] AND c.active <> {true} " +
				"SET c.active = {true} ", new JsonObject().putBoolean("true", true))
				.add(
				"MATCH (c:Conversation) " +
				"WHERE NOT(c.userId IN ['" + Joiner.on("','").join(userIds) + "']) AND c.active <> {false} " + filter +
				"SET c.active = {false} ", disableParams);
		Handler<Message<JsonObject>> h = null;
		if (message != null) {
			h = new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> m) {
					message.reply(m.body());
				}
			};
		}
		neo.executeTransaction(b.build(), null, true, h);
	}

	private void usersInGroups(JsonArray groups, final Handler<JsonArray> users) {
		if (users == null) return;
		if (groups == null) {
			users.handle(null);
		}
		String query =
				"MATCH (g:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE g.id IN {groups} " +
				"RETURN COLLECT(distinct u.id) as users ";
		neo.execute(query, new JsonObject().putArray("groups", groups), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				JsonArray a = message.body().getArray("result");
				if ("ok".equals(message.body().getString("status")) && a != null &&
						a.size() == 1 && a.get(0) != null) {
					JsonObject j = a.get(0);
					users.handle(j.getArray("users"));
				} else {
					users.handle(null);
				}
			}
		});
	}

}
