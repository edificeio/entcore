package org.entcore.conversation.service.impl;

import edu.one.core.infra.collections.Joiner;
import org.entcore.common.appregistry.AppRegistryEventsService;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
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
	public void authorizedActionsUpdated() {
		ApplicationUtils.applicationAllowedUsers(eb, applicationName, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray users) {
				Set<String> userIds = new HashSet<>();
				for (Object o: users) {
					if (!(o instanceof JsonObject)) continue;
					userIds.add(((JsonObject) o).getString("id"));
				}
				manageConversationNodes(userIds);
			}
		});
	}

	private void manageConversationNodes(Set<String> userIds) {
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
				"WHERE NOT(c.userId IN ['" + Joiner.on("','").join(userIds) + "']) AND c.active <> {false} " +
				"SET c.active = {false} ", new JsonObject().putBoolean("false", false));
		neo.executeTransaction(b.build(), null, true, null);
	}

}
